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

class SetResourceLimitsCheckTest {

  private static final String MSG_CPU =
      "Set resources.limits.cpu on this container to keep noisy-neighbour bursts bounded.";
  private static final String MSG_MEM =
      "Set resources.limits.memory on this container to prevent OOM cascades on the node.";
/**
  @Test
  void compliant() {
    K8sYamlVerifier.verifyNoIssue("SetResourceLimitsCheck/compliant.yaml", new SetResourceLimitsCheck());
  }

  @Test
  void noncompliant() {
    // api (line 9): no resources at all → cpu+memory; side (line 11): limits.cpu only → memory missing.
    K8sYamlVerifier.verifyIssues("SetResourceLimitsCheck/noncompliant.yaml",
        new SetResourceLimitsCheck(),
        at(9, MSG_CPU), at(9, MSG_MEM),
        at(11, MSG_MEM));
  }**/
}

