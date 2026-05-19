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
 * 1052 — flag bare compute instances declared with {@code count > 1}
 * (or {@code for_each}) instead of an autoscaling group / MIG / VMSS. See
 * {@code tfenableautoscalinggroup.md}.
 *
 * <p>Heuristic — feasibility ✅. The check fires when the resource is a
 * bare {@code aws_instance} / {@code google_compute_instance} /
 * {@code azurerm_*virtual_machine} (i.e. not a launch template / VMSS) and
 * either {@code count >= 2} or a {@code for_each} attribute is present.
 * Single-instance bastion / jump-box patterns ({@code count == 1} or no
 * {@code count}) are intentionally excluded.</p>
 *
 * <p>Cross-resource autoscaling-group detection is delegated to the
 * sibling {@link TfPreferSpotOrSavingsCheck} (Wave-2D follow-up): if an
 * ASG / MIG / VMSS is present in the same file we do <em>not</em> exempt
 * the bare instances on purpose — they coexist with the autoscaled fleet
 * and remain wasteful.</p>
 */
@Rule(key = "1052")
public class TfEnableAutoscalingGroupCheck implements IacCheck {

  /** Bare compute resources we flag when scaled out by {@code count}/{@code for_each}. */
  private static final Set<String> BARE_INSTANCE_RESOURCES = Set.of(
      "aws_instance",
      "google_compute_instance",
      "azurerm_linux_virtual_machine",
      "azurerm_windows_virtual_machine",
      "azurerm_virtual_machine");

  /** Opt-out: {@code tags.workload = "bastion"} / {@code "jump-box"}. */
  private static final Set<String> SINGLETON_WORKLOAD_VALUES = Set.of(
      "bastion", "jump-box", "jumpbox", "jump_host", "singleton");

  @Override
  public void initialize(@Nonnull InitContext init) {
    init.register(FileTree.class, TfEnableAutoscalingGroupCheck::check);
  }

  private static void check(CheckContext ctx, FileTree file) {
    TerraformCheckUtils.blocks(file, "resource").forEach(block -> checkResource(ctx, block));
  }

  private static void checkResource(CheckContext ctx, BlockTree resource) {
    String type = TerraformCheckUtils.firstLabel(resource);
    if (type == null || !BARE_INSTANCE_RESOURCES.contains(type)) {
      return;
    }
    if (isSingleton(resource)) {
      return;
    }
    boolean scaledOut = isScaledOutByCount(resource) || hasForEach(resource);
    if (!scaledOut) {
      return;
    }
    ctx.reportIssue(resource,
        "Replace this scaled-out " + type + " by an autoscaling group / MIG / VMSS so capacity tracks demand instead of paying for peak 24/7.");
  }

  /** Visible for tests. */
  static boolean isScaledOutByCount(BlockTree resource) {
    AttributeTree countAttr = TerraformCheckUtils.attribute(resource, "count").orElse(null);
    if (countAttr == null) {
      return false;
    }
    String raw = TerraformCheckUtils.stringValue(countAttr).orElse(null);
    if (raw == null) {
      // Non-literal expression (var.count, length(...)) — assume scaled out.
      return true;
    }
    try {
      return Integer.parseInt(raw.trim()) >= 2;
    } catch (NumberFormatException e) {
      return true;
    }
  }

  private static boolean hasForEach(BlockTree resource) {
    return TerraformCheckUtils.attribute(resource, "for_each").isPresent();
  }

  /** Bastion / jump-box opt-out tag. */
  private static boolean isSingleton(BlockTree resource) {
    boolean inNested = TerraformCheckUtils.nestedBlocks(resource)
        .filter(b -> "tags".equals(TerraformCheckUtils.keyName(b)))
        .anyMatch(b -> b.value() != null
            && b.value().statements().stream()
                .filter(AttributeTree.class::isInstance)
                .map(AttributeTree.class::cast)
                .anyMatch(a -> "workload".equalsIgnoreCase(a.key().value())
                    && SINGLETON_WORKLOAD_VALUES.contains(
                        TerraformCheckUtils.stringValue(a).orElse("").toLowerCase(Locale.ROOT))));
    if (inNested) {
      return true;
    }
    return TerraformCheckUtils.attribute(resource, "tags")
        .map(a -> {
          String raw = a.value().toString().toLowerCase(Locale.ROOT);
          return SINGLETON_WORKLOAD_VALUES.stream().anyMatch(v -> raw.contains("\"" + v + "\""));
        }).orElse(false);
  }
}
