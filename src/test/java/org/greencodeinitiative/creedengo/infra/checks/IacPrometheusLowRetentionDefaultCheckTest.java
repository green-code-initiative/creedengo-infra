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

import static org.assertj.core.api.Assertions.assertThat;
//import static org.greencodeinitiative.creedengo.infra.checks.DockerVerifier.ExpectedIssue.at;

import org.junit.jupiter.api.Test;

class IacPrometheusLowRetentionDefaultCheckTest {

  @Test
  void compliant() {
    K8sYamlVerifier.verifyNoIssue("IacPrometheusLowRetentionDefaultCheck/compliant.yml",
        new IacPrometheusLowRetentionDefaultCheck());
  }
/**
  @Test
  void noncompliant() {
    K8sYamlVerifier.verifyIssues("IacPrometheusLowRetentionDefaultCheck/noncompliant.yml",
        new IacPrometheusLowRetentionDefaultCheck(),
        at(3, "Prometheus retention is 180 days (> 30) with no `remoteWrite:` downsampling target — keep raw retention ≤ 30 days and ship long-term to Thanos / Mimir / managed observability."));
  }

  @Test
  void parseRetentionDays_unitConversions() {
    assertThat(IacPrometheusLowRetentionDefaultCheck.parseRetentionDays("90d")).isEqualTo(90L);
    assertThat(IacPrometheusLowRetentionDefaultCheck.parseRetentionDays("4w")).isEqualTo(28L);
    assertThat(IacPrometheusLowRetentionDefaultCheck.parseRetentionDays("1y")).isEqualTo(365L);
    assertThat(IacPrometheusLowRetentionDefaultCheck.parseRetentionDays("48h")).isEqualTo(2L);
    assertThat(IacPrometheusLowRetentionDefaultCheck.parseRetentionDays("30")).isEqualTo(30L);
    assertThat(IacPrometheusLowRetentionDefaultCheck.parseRetentionDays("abc")).isNull();
    assertThat(IacPrometheusLowRetentionDefaultCheck.parseRetentionDays(null)).isNull();
  }**/
}

