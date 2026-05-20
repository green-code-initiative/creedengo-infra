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

import org.junit.jupiter.api.Test;
import org.sonar.iac.commons.testing.Verifier;

class SetResourceRequestsAndLimitsCheckTest {

  private static final SetResourceRequestsAndLimitsCheck CHECK = new SetResourceRequestsAndLimitsCheck();

  @Test
  void deployment_compliant() {
    KubernetesVerifier.verifyNoIssue(
        "SetResourceRequestsAndLimitsCheck/compliant_deployment.yaml", CHECK);
  }

  @Test
  void cronjob_compliant() {
    KubernetesVerifier.verifyNoIssue(
        "SetResourceRequestsAndLimitsCheck/compliant_cronjob.yaml", CHECK);
  }

  @Test
  void no_resources_block() {
    KubernetesVerifier.verify(
        "SetResourceRequestsAndLimitsCheck/noncompliant_no_resources.yaml", CHECK);
  }

  @Test
  void missing_requests_block() {
    KubernetesVerifier.verify(
        "SetResourceRequestsAndLimitsCheck/noncompliant_missing_requests.yaml", CHECK);
  }

  @Test
  void missing_requests_fields() {
    KubernetesVerifier.verify(
        "SetResourceRequestsAndLimitsCheck/noncompliant_missing_requests_fields.yaml", CHECK);
  }

  @Test
  void missing_limits_block() {
    KubernetesVerifier.verify(
        "SetResourceRequestsAndLimitsCheck/noncompliant_missing_limits.yaml", CHECK);
  }

  @Test
  void missing_limits_fields() {
    KubernetesVerifier.verify(
        "SetResourceRequestsAndLimitsCheck/noncompliant_missing_limits_fields.yaml", CHECK);
  }

  @Test
  void cronjob_missing_limits_cpu() {
    KubernetesVerifier.verify(
        "SetResourceRequestsAndLimitsCheck/noncompliant_cronjob_missing_cpu_limit.yaml", CHECK);
  }
}