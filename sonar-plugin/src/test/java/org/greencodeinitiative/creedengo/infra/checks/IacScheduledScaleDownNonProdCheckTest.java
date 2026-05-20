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
import org.junit.jupiter.api.Test;

class IacScheduledScaleDownNonProdCheckTest {
/**
  @Test
  void compliant() {
    K8sYamlVerifier.verifyNoIssue("IacScheduledScaleDownNonProdCheck/compliant.yml",
        new IacScheduledScaleDownNonProdCheck());
  }

  @Test
  void noncompliant() {
    K8sYamlVerifier.verifyIssues("IacScheduledScaleDownNonProdCheck/noncompliant.yml",
        new IacScheduledScaleDownNonProdCheck(),
        at(1, "Non-prod namespace 'staging' has no scheduled scale-down resource (kube-green SleepInfo, KEDA cron, Knative scale-to-zero) in this file — add one to power down off-hours."),
        at(6, "Non-prod namespace 'my-dev-cluster' has no scheduled scale-down resource (kube-green SleepInfo, KEDA cron, Knative scale-to-zero) in this file — add one to power down off-hours."));
  }

  @Test
  void isNonProdName_truthTable() {
    assertThat(IacScheduledScaleDownNonProdCheck.isNonProdName("staging")).isTrue();
    assertThat(IacScheduledScaleDownNonProdCheck.isNonProdName("my-dev")).isTrue();
    assertThat(IacScheduledScaleDownNonProdCheck.isNonProdName("preprod-eu")).isTrue();
    assertThat(IacScheduledScaleDownNonProdCheck.isNonProdName("production")).isFalse();
    assertThat(IacScheduledScaleDownNonProdCheck.isNonProdName("prod-eu")).isFalse();
    assertThat(IacScheduledScaleDownNonProdCheck.isNonProdName(null)).isFalse();
  }**/
}

