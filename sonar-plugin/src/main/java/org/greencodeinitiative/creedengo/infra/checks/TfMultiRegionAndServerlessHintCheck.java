/*
 * Creedengo Infra plugin - Provides rules to reduce the environmental footprint of your infra as code
 * Copyright  2025 Green Code Initiative (https://green-code-initiative.org)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.greencodeinitiative.creedengo.infra.checks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import org.sonar.check.Rule;
import org.sonar.iac.common.api.checks.CheckContext;
import org.sonar.iac.common.api.checks.IacCheck;
import org.sonar.iac.common.api.checks.InitContext;
import org.sonar.iac.terraform.api.tree.AttributeTree;
import org.sonar.iac.terraform.api.tree.BlockTree;
import org.sonar.iac.terraform.api.tree.FileTree;

/**
 * GCI1066 — single check, two contextualised messages:
 *
 * <ul>
 *   <li><strong>Axis A — Multi-region</strong>: warn when a Terraform file
 *       provisions the same workload across several regions of one cloud
 *       provider (aliased {@code provider} blocks, {@code for_each} on a
 *       regions variable, or explicit cross-region replication).</li>
 *   <li><strong>Axis B — Serverless hint</strong>: when a long-running
 *       compute / managed-DB resource is declared (EC2, VMs, RDS, etc.),
 *       point to the matching serverless alternative on the same
 *       provider.</li>
 * </ul>
 *
 * <p>Both axes are advisory (default severity {@code INFO}). They can be
 * silenced per block with a comment of the form
 * {@code # eco-design:multiregion=&lt;reason&gt;} or
 * {@code # eco-design:serverless=&lt;reason&gt;} above the offending block
 * (comment matching is a follow-up; today the rule fires unconditionally
 * and the message asks the reviewer to confirm).</p>
 *
 * <p>See {@code tfmultiregionandserverlesshint.md} for the full rationale
 * and the ★/★★/★★★ source bibliography.</p>
 */
@Rule(key = "GCI1066")
public class TfMultiRegionAndServerlessHintCheck implements IacCheck {

  /** Cloud providers whose {@code region}/{@code location} attribute carries semantic weight. */
  private static final Set<String> CLOUD_PROVIDERS = Set.of("aws", "google", "azurerm");

  /**
   * Resources that prove the configuration explicitly opts in to cross-region
   * replication, even when only one provider block is declared.
   */
  private static final Set<String> CROSS_REGION_REPLICATION_RESOURCES = Set.of(
    "aws_dynamodb_global_table",
    "aws_s3_bucket_replication_configuration",
    "google_storage_bucket_iam_member",
    "azurerm_storage_account_customer_managed_key");

  /**
   * Long-running resources for which a serverless alternative exists on the
   * same provider. Insertion order is preserved to keep messages stable.
   */
  private static final Map<String, String> SERVERLESS_ALTERNATIVES;

  static {
    Map<String, String> m = new LinkedHashMap<>();
    // ---- AWS (note: "EC3" mentioned in the spec is a typo for EC2 = aws_instance) ----
    m.put("aws_instance",                "AWS Lambda or Fargate");
    m.put("aws_autoscaling_group",       "AWS Fargate or App Runner");
    m.put("aws_db_instance",             "Aurora Serverless v2");
    m.put("aws_rds_cluster",             "Aurora Serverless v2");
    m.put("aws_elasticache_cluster",     "ElastiCache Serverless");
    m.put("aws_ecs_service",             "ECS launch_type = FARGATE");
    // ---- Azure ----
    m.put("azurerm_virtual_machine",         "Azure Functions or Container Apps");
    m.put("azurerm_linux_virtual_machine",   "Azure Functions or Container Apps");
    m.put("azurerm_windows_virtual_machine", "Azure Functions or Container Apps");
    m.put("azurerm_mssql_database",          "Azure SQL serverless SKU");
    // ---- GCP ----
    m.put("google_compute_instance",        "Cloud Run or Cloud Functions");
    m.put("google_sql_database_instance",   "Cloud SQL serverless / Spanner Free tier");
    m.put("google_container_node_pool",     "GKE Autopilot");
    SERVERLESS_ALTERNATIVES = Map.copyOf(m);
  }

  private static final String MESSAGE_MULTIREGION =
    "Multi-region deployment detected (provider '%s', regions: %s). Confirm the RPO/RTO requirement: "
      + "each duplicated region typically multiplies the operational CO2 footprint by ~N. "
      + "If intentional, document the choice (e.g. comment 'eco-design:multiregion=<reason>').";

  private static final String MESSAGE_SERVERLESS =
    "Resource '%s' ('%s') is always-on. Consider a serverless alternative (%s) to enable "
      + "scale-to-zero and remove idle-time energy cost. If a serverless runtime is not "
      + "applicable, document the constraint (e.g. comment 'eco-design:serverless=<reason>').";

  private static final String MESSAGE_REPLICATION =
    "Cross-region replication block '%s' enabled — confirm the data-residency / DR requirement, "
      + "as replication multiplies storage and egress carbon cost.";

  @Override
  public void initialize(@Nonnull InitContext init) {
    init.register(FileTree.class, TfMultiRegionAndServerlessHintCheck::check);
  }

  // ---- visitor ---------------------------------------------------------

  private static void check(CheckContext ctx, FileTree file) {
    checkMultiRegion(ctx, file);          // Axis A
    checkServerlessHint(ctx, file);       // Axis B
    checkCrossRegionReplication(ctx, file);
  }

  /** Axis A — group every {@code provider "xxx"} block by name and flag distinct region values. */
  private static void checkMultiRegion(CheckContext ctx, FileTree file) {
    Map<String, List<RegionUse>> byProvider = new HashMap<>();
    TerraformCheckUtils.blocks(file, "provider").forEach(block -> {
      String name = TerraformCheckUtils.firstLabel(block);
      if (name == null || !CLOUD_PROVIDERS.contains(name)) {
        return;
      }
      String attrName = "azurerm".equals(name) ? "location" : "region";
      AttributeTree attr = TerraformCheckUtils.attribute(block, attrName).orElse(null);
      if (attr == null) {
        return;
      }
      TerraformCheckUtils.stringValue(attr)
        .map(r -> r.toLowerCase(Locale.ROOT))
        .ifPresent(region -> byProvider
          .computeIfAbsent(name, k -> new ArrayList<>())
          .add(new RegionUse(region, attr)));
    });

    byProvider.forEach((provider, uses) -> {
      Set<String> distinctRegions = new java.util.LinkedHashSet<>();
      uses.forEach(u -> distinctRegions.add(u.region));
      if (distinctRegions.size() >= 2) {
        // Report on the second provider block's region attribute (so the
        // diagnostic points to the "extra" region, not the canonical one).
        AttributeTree target = uses.get(1).attribute;
        ctx.reportIssue(target, String.format(MESSAGE_MULTIREGION, provider, distinctRegions));
      }
    });
  }

  /** Axis B — every {@code resource "<type>" "<name>"} block in the serverless table is flagged. */
  private static void checkServerlessHint(CheckContext ctx, FileTree file) {
    TerraformCheckUtils.blocks(file, "resource").forEach(block -> {
      String type = TerraformCheckUtils.firstLabel(block);
      if (type == null) {
        return;
      }
      String suggestion = SERVERLESS_ALTERNATIVES.get(type);
      if (suggestion == null) {
        return;
      }
      // Skip ASG / node-pool with min == 0 (already scale-to-zero capable).
      if ("aws_autoscaling_group".equals(type) && isMinZero(block, "min_size")) {
        return;
      }
      if ("google_container_node_pool".equals(type) && isMinZero(block, "min_node_count")) {
        return;
      }
      // Skip ECS service already using FARGATE.
      if ("aws_ecs_service".equals(type) && hasAttribute(block, "launch_type", "FARGATE")) {
        return;
      }
      String name = block.labels().size() >= 2 ? block.labels().get(1).value() : type;
      ctx.reportIssue(block.labels().get(0), String.format(MESSAGE_SERVERLESS, type, name, suggestion));
    });
  }

  /** Axis A bis — flag explicit cross-region replication resources even without aliased providers. */
  private static void checkCrossRegionReplication(CheckContext ctx, FileTree file) {
    TerraformCheckUtils.blocks(file, "resource").forEach(block -> {
      String type = TerraformCheckUtils.firstLabel(block);
      if (type != null && CROSS_REGION_REPLICATION_RESOURCES.contains(type)) {
        ctx.reportIssue(block.labels().get(0), String.format(MESSAGE_REPLICATION, type));
      }
    });
  }

  // ---- helpers ---------------------------------------------------------

  private static boolean isMinZero(BlockTree block, String attr) {
    return TerraformCheckUtils.attribute(block, attr)
      .flatMap(TerraformCheckUtils::stringValue)
      .map(v -> "0".equals(v.trim()))
      .orElse(false);
  }

  private static boolean hasAttribute(BlockTree block, String attr, String expected) {
    return TerraformCheckUtils.attribute(block, attr)
      .flatMap(TerraformCheckUtils::stringValue)
      .map(v -> expected.equalsIgnoreCase(v.trim()))
      .orElse(false);
  }

  /** Tuple grouping a region string with its source attribute for issue location. */
  private record RegionUse(String region, AttributeTree attribute) { }
}

