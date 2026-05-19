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
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.iac.common.api.checks.CheckContext;
import org.sonar.iac.common.api.checks.IacCheck;
import org.sonar.iac.common.api.checks.InitContext;
import org.sonar.iac.common.yaml.tree.FileTree;
import org.sonar.iac.common.yaml.tree.MappingTree;

/**
 * GCI1042 — for each container, the ratio
 * {@code limits.<resource> / requests.<resource>} should stay below
 * {@link #maxRatio} (default 4). High ratios signal chronic
 * over-allocation and break HPA / Karpenter decisions — see
 * {@code cpurequestvslimitratio.md}.
 *
 * <p>Resources covered: {@code cpu} and {@code memory}. Quantities are
 * parsed with the standard Kubernetes suffix table (m, Ki, Mi, Gi, …).
 * Unparseable values are skipped (we err on the side of no false
 * positives).</p>
 */
@Rule(key = "GCI1042")
public class CpuRequestVsLimitRatioCheck implements IacCheck {

  private static final double DEFAULT_RATIO = 4.0;

  @RuleProperty(
      key = "maxRatio",
      description = "Maximum allowed limits/requests ratio per resource (cpu, memory).",
      defaultValue = "4")
  public double maxRatio = DEFAULT_RATIO;

  @Override
  public void initialize(@javax.annotation.Nonnull InitContext init) {
    init.register(FileTree.class, this::check);
  }

  private void check(CheckContext ctx, FileTree file) {
    KubernetesCheckUtils.documents(file).forEach(doc ->
        KubernetesCheckUtils.podSpec(doc).ifPresent(spec ->
            KubernetesCheckUtils.containers(spec).forEach(container -> checkContainer(ctx, container))));
  }

  private void checkContainer(CheckContext ctx, MappingTree container) {
    Optional<MappingTree> resources = KubernetesCheckUtils.mapping(container, "resources");
    if (resources.isEmpty()) {
      return;
    }
    Optional<MappingTree> requests = KubernetesCheckUtils.mapping(resources.get(), "requests");
    Optional<MappingTree> limits = KubernetesCheckUtils.mapping(resources.get(), "limits");
    if (requests.isEmpty() || limits.isEmpty()) {
      return;
    }
    checkResource(ctx, container, requests.get(), limits.get(), "cpu");
    checkResource(ctx, container, requests.get(), limits.get(), "memory");
  }

  private void checkResource(CheckContext ctx, MappingTree container,
      MappingTree requests, MappingTree limits, String name) {
    Optional<Double> req = KubernetesCheckUtils.scalar(requests, name).flatMap(v -> parseQuantity(name, v));
    Optional<Double> lim = KubernetesCheckUtils.scalar(limits, name).flatMap(v -> parseQuantity(name, v));
    if (req.isEmpty() || lim.isEmpty() || req.get() <= 0) {
      return;
    }
    double ratio = lim.get() / req.get();
    if (ratio > maxRatio) {
      ctx.reportIssue(container,
          String.format(Locale.ROOT,
              "Ratio %s limits/requests is %.1fx (max %.1fx): rightsize to avoid throttling and HPA flapping.",
              name, ratio, maxRatio));
    }
  }

  /**
   * Parse a Kubernetes quantity. CPU accepts the {@code m} milli suffix;
   * memory accepts the binary (Ki/Mi/Gi/Ti/Pi/Ei) and decimal (K/M/G/T/P/E)
   * suffixes plus the {@code e} exponent form. Returns empty on unknown
   * formats so the rule degrades to no-issue instead of raising false
   * positives.
   */
  static Optional<Double> parseQuantity(String resource, String raw) {
    if (raw == null) {
      return Optional.empty();
    }
    String s = raw.trim();
    if (s.isEmpty()) {
      return Optional.empty();
    }
    try {
      if ("cpu".equals(resource)) {
        if (s.endsWith("m")) {
          return Optional.of(Double.parseDouble(s.substring(0, s.length() - 1)) / 1000.0);
        }
        return Optional.of(Double.parseDouble(s));
      }
      // memory
      long multiplier = 1L;
      String num = s;
      String[][] suffixes = {
          {"Ei", "1152921504606846976"}, {"Pi", "1125899906842624"}, {"Ti", "1099511627776"},
          {"Gi", "1073741824"}, {"Mi", "1048576"}, {"Ki", "1024"},
          {"E", "1000000000000000000"}, {"P", "1000000000000000"}, {"T", "1000000000000"},
          {"G", "1000000000"}, {"M", "1000000"}, {"K", "1000"}, {"k", "1000"}
      };
      for (String[] pair : suffixes) {
        if (s.endsWith(pair[0])) {
          num = s.substring(0, s.length() - pair[0].length());
          multiplier = Long.parseLong(pair[1]);
          break;
        }
      }
      return Optional.of(Double.parseDouble(num) * multiplier);
    } catch (NumberFormatException e) {
      return Optional.empty();
    }
  }
}
