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

import static org.greencodeinitiative.creedengo.infra.checks.DockerVerifier.ExpectedIssue.at;

import org.junit.jupiter.api.Test;

class SetResourceRequestsCheckTest {

  private static final String MSG_CPU =
      "Set resources.requests.cpu on this container for accurate scheduler bin-packing.";
  private static final String MSG_MEM =
      "Set resources.requests.memory on this container for accurate scheduler bin-packing.";

  @Test
  void compliant() {
    K8sYamlVerifier.verifyNoIssue("SetResourceRequestsCheck/compliant.yaml", new SetResourceRequestsCheck());
  }

  @Test
  void noncompliant() {
    // Containers: api (line 9) → cpu+memory missing,
    //            worker (line 11) → memory missing,
    //            solo Pod (line 23) → cpu+memory missing.
    K8sYamlVerifier.verifyIssues("SetResourceRequestsCheck/noncompliant.yaml",
        new SetResourceRequestsCheck(),
        at(9, MSG_CPU), at(9, MSG_MEM),
        at(11, MSG_MEM),
        at(23, MSG_CPU), at(23, MSG_MEM));
  }
}

