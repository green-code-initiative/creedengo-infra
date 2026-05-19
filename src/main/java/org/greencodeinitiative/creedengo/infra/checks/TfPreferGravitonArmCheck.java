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

import java.util.List;
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
 * GCI1050 — flag compute resources pinned to an x86 instance family when
 * an ARM (Graviton / Tau T2A / Cobalt 100) equivalent exists. See
 * {@code tfprefergravitonarm.md}. The check is conservative: it only
 * flags families for which a documented ARM equivalent is available, and
 * only when the instance type is a plain string literal (interpolated
 * values are skipped to avoid false positives).
 */
@Rule(key = "GCI1050")
public class TfPreferGravitonArmCheck implements IacCheck {

  /**
   * Resources whose {@code instance_type} attribute should be ARM-aware.
   * Multi-cloud — AWS uses {@code instance_type}, GCP {@code machine_type},
   * Azure {@code vm_size}; we keep the simple AWS-style for v1.
   */
  private static final Set<String> AWS_INSTANCE_RESOURCES = Set.of(
    "aws_instance", "aws_launch_template", "aws_launch_configuration",
    "aws_db_instance", "aws_ecs_service");

  /**
   * x86 → ARM-equivalent family. Source: AWS Graviton family chart 2025-Q4.
   */
  private static final java.util.Map<String, String> X86_TO_ARM = java.util.Map.ofEntries(
    java.util.Map.entry("m5",   "m6g/m7g"),
    java.util.Map.entry("m5a",  "m6g/m7g"),
    java.util.Map.entry("m6i",  "m7g"),
    java.util.Map.entry("m6a",  "m7g"),
    java.util.Map.entry("m7i",  "m7g"),
    java.util.Map.entry("c5",   "c6g/c7g"),
    java.util.Map.entry("c5a",  "c6g/c7g"),
    java.util.Map.entry("c6i",  "c7g"),
    java.util.Map.entry("c6a",  "c7g"),
    java.util.Map.entry("c7i",  "c7g"),
    java.util.Map.entry("r5",   "r6g/r7g"),
    java.util.Map.entry("r5a",  "r6g/r7g"),
    java.util.Map.entry("r6i",  "r7g"),
    java.util.Map.entry("r7i",  "r7g"),
    java.util.Map.entry("t2",   "t4g"),
    java.util.Map.entry("t3",   "t4g"),
    java.util.Map.entry("t3a",  "t4g"));

  @Override
  public void initialize(@Nonnull InitContext init) {
    init.register(FileTree.class, TfPreferGravitonArmCheck::check);
  }

  private static void check(CheckContext ctx, FileTree file) {
    TerraformCheckUtils.blocks(file, "resource").forEach(block -> checkResource(ctx, block));
  }

  private static void checkResource(CheckContext ctx, BlockTree resource) {
    String type = TerraformCheckUtils.firstLabel(resource);
    if (type == null || !AWS_INSTANCE_RESOURCES.contains(type)) {
      return;
    }
    AttributeTree instanceType = TerraformCheckUtils.attribute(resource, "instance_type").orElse(null);
    if (instanceType == null) {
      return;
    }
    String value = TerraformCheckUtils.stringValue(instanceType).orElse(null);
    if (value == null) {
      return;
    }
    String family = parseFamily(value);
    String arm = X86_TO_ARM.get(family);
    if (arm != null) {
      ctx.reportIssue(instanceType, "Prefer the ARM/Graviton equivalent of " + family + " (e.g. " + arm + ") for better watt-per-request.");
    }
  }

  /** {@code "m6i.large"} → {@code "m6i"}. Empty / null → empty. */
  static String parseFamily(String instanceType) {
    if (instanceType == null) {
      return "";
    }
    int dot = instanceType.indexOf('.');
    return dot < 0 ? instanceType : instanceType.substring(0, dot);
  }

  /** Visible for tests. */
  static List<String> knownX86Families() {
    return List.copyOf(X86_TO_ARM.keySet());
  }
}
