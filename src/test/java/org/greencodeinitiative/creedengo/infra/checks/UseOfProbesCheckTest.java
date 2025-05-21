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
import org.sonar.iac.common.api.checks.IacCheck;

class UseOfProbesCheckTest {

  IacCheck check = new UseOfProbesCheck();

  @Test
  void shouldNotRaiseWhenAllProbes() {
    KubernetesVerifier.verifyNoIssue("UseOfProbesCheck/helm/templates/all_probes.yaml", check);
  }

  @Test
  void shouldRaiseWhenNoProbe() {
    KubernetesVerifier.verify("UseOfProbesCheck/helm/templates/no_probes.yaml", check);
  }

  @Test
  void shouldRaiseWhenOnlyLivenessProbe() {
    KubernetesVerifier.verify("UseOfProbesCheck/helm/templates/liveness_probes.yaml", check);
  }

  @Test
  void shouldRaiseWhenOnlyReadinessProbe() {
    KubernetesVerifier.verify("UseOfProbesCheck/helm/templates/readiness_probes.yaml", check);
  }
}
