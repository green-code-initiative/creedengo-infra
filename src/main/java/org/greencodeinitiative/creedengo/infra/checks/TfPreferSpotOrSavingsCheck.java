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

import java.util.Locale;
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
 * 1056 — flag fault-tolerant pools that do not mix Spot / Preemptible /
 * Low-Priority capacity. See {@code tfpreferspotorsavings.md}.
 *
 * <p>Heuristic — feasibility ⚠️. Covers AWS Autoscaling Groups, GCP MIGs,
 * Azure VM Scale Sets. Opt-out: tag {@code workload=stateful} on the
 * resource (so latency-critical fleets aren't flagged).</p>
 */
@Rule(key = "1056")
public class TfPreferSpotOrSavingsCheck implements IacCheck {

  /** Target group resources, mapped to per-provider spot-detection logic. */
  private static final Set<String> AWS_ASG = Set.of("aws_autoscaling_group");
  private static final Set<String> GCP_MIG = Set.of(
      "google_compute_instance_group_manager",
      "google_compute_region_instance_group_manager");
  private static final Set<String> AZURE_VMSS = Set.of("azurerm_linux_virtual_machine_scale_set",
      "azurerm_windows_virtual_machine_scale_set");

  private static final Set<String> STATEFUL_TAG_KEYS = Set.of("workload", "tier");
  private static final Set<String> STATEFUL_TAG_VALUES = Set.of(
      "stateful", "database", "stateful-db", "latency-critical");

  @Override
  public void initialize(@Nonnull InitContext init) {
    //init.register(FileTree.class, TfPreferSpotOrSavingsCheck::check);
  }
}
