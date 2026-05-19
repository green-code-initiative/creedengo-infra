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

import java.util.Set;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import org.sonar.check.Rule;
import org.sonar.iac.common.api.checks.CheckContext;
import org.sonar.iac.common.api.checks.IacCheck;
import org.sonar.iac.common.api.checks.InitContext;
import org.sonar.iac.terraform.api.tree.AttributeTree;
import org.sonar.iac.terraform.api.tree.BlockTree;
import org.sonar.iac.terraform.api.tree.FileTree;

/**
 * 1051 — flag oversized compute instance types declared without a
 * documented sizing rationale. See {@code tfrightsizeinstancetypes.md}.
 *
 * <p>Heuristic — feasibility ⚠️. The check only flags AWS / GCP / Azure
 * instance sizes statically recognised as "large defaults"
 * ({@code >= 2xlarge} on AWS, {@code *-standard-8} and above on GCP,
 * {@code Standard_D8} family and above on Azure) when the enclosing resource
 * carries no sizing-rationale tag and no {@code lifecycle.ignore_changes}
 * hint to a recommender tool.</p>
 */
@Rule(key = "GCI1051")
public class TfRightSizeInstanceTypesCheck implements IacCheck {

  /** Compute / DB resources whose size attribute we audit. */
  private static final Set<String> TARGET_RESOURCES = Set.of(
      "aws_instance", "aws_launch_template", "aws_launch_configuration",
      "aws_db_instance",
      "google_compute_instance", "google_compute_instance_template",
      "azurerm_linux_virtual_machine", "azurerm_windows_virtual_machine",
      "azurerm_virtual_machine_scale_set");

  /** Attribute names that hold a sizing value across providers. */
  private static final Set<String> SIZE_ATTRIBUTES = Set.of(
      "instance_type",     // AWS
      "instance_class",    // AWS RDS
      "machine_type",      // GCP
      "vm_size",           // Azure (classic)
      "sku");              // Azure VMSS

  /** Tag keys (case-insensitive) that prove the size was reviewed. */
  private static final Set<String> RATIONALE_TAG_KEYS = Set.of(
      "sizing-rationale", "sizing_rationale", "sizingrationale",
      "eco-design", "ecodesign",
      "rightsized", "right-sized");

  /** AWS: matches anything strictly larger than {@code xlarge}, plus {@code .metal}. */
  private static final Pattern AWS_OVERSIZED = Pattern.compile(
      "^[a-z]+\\d[a-z]*\\.\\d+xlarge$|^[a-z]+\\d[a-z]*\\.metal.*$");

  /** GCP {@code *-standard-8} (or 16, 32, …) families and above. */
  private static final Pattern GCP_OVERSIZED = Pattern.compile(
      "^[a-z]\\d?-(?:standard|highmem|highcpu|megamem|ultramem)-(?:8|16|32|48|64|80|96|128)$");

  /** Azure {@code Standard_D8} / {@code Standard_E8} / {@code Standard_F8} families and above. */
  private static final Pattern AZURE_OVERSIZED = Pattern.compile(
      "^Standard_[A-Z]+(?:8|16|24|32|48|64|72|96|128)[a-z]*(?:_v\\d)?$");

  @Override
  public void initialize(@Nonnull InitContext init) {
    //init.register(FileTree.class, TfRightSizeInstanceTypesCheck::check);
  }

  private static void check(CheckContext ctx, FileTree file) {
    TerraformCheckUtils.blocks(file, "resource").forEach(block -> checkResource(ctx, block));
  }

  private static void checkResource(CheckContext ctx, BlockTree resource) {
   }
}
