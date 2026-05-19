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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import org.sonar.check.Rule;
import org.sonar.iac.common.api.checks.CheckContext;
import org.sonar.iac.common.api.checks.IacCheck;
import org.sonar.iac.common.api.checks.InitContext;
import org.sonar.iac.common.yaml.tree.MappingTree;
import org.sonar.iac.common.yaml.tree.TupleTree;

/**
 * GCI1063 — flag Prometheus deployments / Helm values where raw retention
 * exceeds {@value #RETENTION_MAX_DAYS} days without an accompanying
 * {@code remoteWrite:} block. See {@code iacprometheuslowretentiondefault.md}.
 *
 * <p>Detection scope:</p>
 * <ul>
 *   <li>Helm values style: any mapping with a {@code retention:} scalar
 *       sibling of a {@code scrapeInterval:} or {@code retentionSize:}.</li>
 *   <li>Operator CR style: any mapping with both {@code retention:} and
 *       sibling {@code scrapeInterval:}/{@code replicas:}/{@code resources:}
 *       (heuristic for {@code prometheusSpec}).</li>
 * </ul>
 *
 * <p>The check visits every {@link MappingTree}, so deeply-nested Helm
 * values such as {@code prometheus.prometheusSpec.retention} are picked up
 * regardless of their position.</p>
 */
@Rule(key = "GCI1063")
public class IacPrometheusLowRetentionDefaultCheck implements IacCheck {

  /** Threshold above which the rule fires when no downsampling exists. */
  static final int RETENTION_MAX_DAYS = 30;

  /** Sibling keys that indicate this mapping really is a Prometheus spec. */
  private static final java.util.Set<String> SIBLING_HINTS = java.util.Set.of(
      "scrapeInterval", "scrape_interval",
      "retentionSize", "retention_size",
      "replicas", "resources",
      "remoteWrite", "remote_write",
      "evaluationInterval", "evaluation_interval");

  private static final Pattern RETENTION_VALUE = Pattern.compile(
      "^\\s*(\\d+)\\s*([smhdwy]?)\\s*$", Pattern.CASE_INSENSITIVE);

  @Override
  public void initialize(@Nonnull InitContext init) {
    init.register(MappingTree.class, IacPrometheusLowRetentionDefaultCheck::check);
  }

  private static void check(CheckContext ctx, MappingTree mapping) {
    TupleTree retentionTuple = KubernetesCheckUtils.tuple(mapping, "retention").orElse(null);
    if (retentionTuple == null) {
      return;
    }
    if (!looksLikePrometheusSpec(mapping)) {
      return;
    }
    String retentionRaw = KubernetesCheckUtils.asScalarString(retentionTuple.value()).orElse(null);
    Long days = parseRetentionDays(retentionRaw);
    if (days == null || days <= RETENTION_MAX_DAYS) {
      return;
    }
    if (hasRemoteWrite(mapping)) {
      return;
    }
    ctx.reportIssue(retentionTuple,
        String.format(Locale.ROOT,
            "Prometheus retention is %d days (> %d) with no `remoteWrite:` downsampling target — keep raw retention ≤ %d days and ship long-term to Thanos / Mimir / managed observability.",
            days, RETENTION_MAX_DAYS, RETENTION_MAX_DAYS));
  }

  /** Visible for tests. {@code 90d} → 90; {@code 4w} → 28; null/blank → null. */
  static Long parseRetentionDays(String raw) {
    if (raw == null) {
      return null;
    }
    Matcher m = RETENTION_VALUE.matcher(raw);
    if (!m.matches()) {
      return null;
    }
    long value = Long.parseLong(m.group(1));
    String unit = m.group(2).toLowerCase(Locale.ROOT);
    return switch (unit) {
      case "", "d" -> value;
      case "h" -> Math.max(1, value / 24);
      case "m" -> Math.max(1, value / (24 * 60));
      case "s" -> Math.max(1, value / (24 * 3600));
      case "w" -> value * 7;
      case "y" -> value * 365;
      default -> value;
    };
  }

  private static boolean looksLikePrometheusSpec(MappingTree mapping) {
    for (TupleTree t : mapping.elements()) {
      String key = KubernetesCheckUtils.asScalarString(t.key()).orElse("");
      if (SIBLING_HINTS.contains(key)) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasRemoteWrite(MappingTree mapping) {
    return KubernetesCheckUtils.tuple(mapping, "remoteWrite").isPresent()
        || KubernetesCheckUtils.tuple(mapping, "remote_write").isPresent();
  }
}
