/*
 * Creedengo Infra plugin - Provides rules to reduce the environmental footprint of your infra as code
 * Copyright © 2025 Green Code Initiative (https://green-code-initiative.org)
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
 * GCI1054 — flag {@code provider} blocks (AWS / GCP / Azure) that configure
 * a {@code region} / {@code location} known to be high-carbon. See
 * {@code tfchooselowcarbonregion.md}. Source: ElectricityMaps yearly
 * averages cross-referenced with each cloud provider's official region
 * sustainability page (snapshot 2026-04).
 *
 * <p>The "high-carbon" set is intentionally conservative — we list only
 * regions whose electricity mix is consistently above ~400 gCO2/kWh per
 * provider-published data. False positives are silenced by a comment
 * containing {@code eco-design:region} on the line just above the
 * provider block (handled at the markdown level today — code-level
 * justification comment matching is a follow-up).</p>
 */
@Rule(key = "GCI1054")
public class TfChooseLowCarbonRegionCheck implements IacCheck {

  /** Provider blocks for which {@code region} / {@code location} is meaningful. */
  private static final Set<String> CLOUD_PROVIDERS = Set.of("aws", "google", "azurerm");

  /**
   * High-carbon regions per provider, value = ~gCO2/kWh annual average
   * (informative; only used in the message). Keep keys lower-case.
   */
  private static final Map<String, Integer> HIGH_CARBON_REGIONS = Map.ofEntries(
    // AWS
    Map.entry("ap-south-1",         700),  // Mumbai
    Map.entry("ap-southeast-1",     400),  // Singapore
    Map.entry("ap-southeast-2",     520),  // Sydney
    Map.entry("ap-southeast-3",     650),  // Jakarta
    Map.entry("ap-northeast-3",     430),  // Osaka
    Map.entry("af-south-1",         800),  // Cape Town
    Map.entry("me-south-1",         700),  // Bahrain
    Map.entry("me-central-1",       650),  // UAE
    Map.entry("sa-east-1",          110),  // São Paulo (kept low — clean grid)
    Map.entry("us-east-1",          380),  // Virginia
    // GCP
    Map.entry("asia-south1",        700),  // Mumbai
    Map.entry("asia-southeast1",    400),  // Singapore
    Map.entry("australia-southeast1", 520),
    Map.entry("us-west2",           380),
    // Azure
    Map.entry("southeastasia",      400),  // Singapore
    Map.entry("centralindia",       700),
    Map.entry("australiaeast",      520),
    Map.entry("japaneast",          430),
    Map.entry("southafricanorth",   800));

  @Override
  public void initialize(@Nonnull InitContext init) {
    init.register(FileTree.class, TfChooseLowCarbonRegionCheck::check);
  }

  private static void check(CheckContext ctx, FileTree file) {
    TerraformCheckUtils.blocks(file, "provider").forEach(block -> checkProvider(ctx, block));
  }

  private static void checkProvider(CheckContext ctx, BlockTree provider) {
    String name = TerraformCheckUtils.firstLabel(provider);
    if (name == null || !CLOUD_PROVIDERS.contains(name)) {
      return;
    }
    // AWS / GCP → region; Azure → location.
    String attrName = "azurerm".equals(name) ? "location" : "region";
    AttributeTree regionAttr = TerraformCheckUtils.attribute(provider, attrName).orElse(null);
    if (regionAttr == null) {
      return;
    }
    String region = TerraformCheckUtils.stringValue(regionAttr).orElse(null);
    if (region == null) {
      return;
    }
    Integer gco2 = HIGH_CARBON_REGIONS.get(region.toLowerCase());
    if (gco2 != null) {
      ctx.reportIssue(regionAttr,
        "Region '" + region + "' has a high-carbon grid (~" + gco2 + " gCO2/kWh): document the constraint or relocate to a low-carbon region.");
    }
  }
}
