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

//import static org.greencodeinitiative.creedengo.infra.checks.DockerVerifier.ExpectedIssue.at;

import org.junit.jupiter.api.Test;

class MergeConsecutiveRunCheckTest {

  private static final String MESSAGE =
      "Merge this RUN with the previous one (using && and \\) to keep layers small and let cleanup happen in the same layer.";
/**
  @Test
  void compliant() {
    DockerVerifier.verifyNoIssue("MergeConsecutiveRunCheck/compliant.Dockerfile", new MergeConsecutiveRunCheck());
  }

  @Test
  void noncompliant() {
    // RUN at lines 2,3,4,6,7. Reports second-and-subsequent in each consecutive group:
    //   group #1: 2,3,4 → flag 3 and 4
    //   COPY at line 5 breaks the chain
    //   group #2: 6,7 → flag 7
    DockerVerifier.verifyIssues("MergeConsecutiveRunCheck/noncompliant.Dockerfile",
        new MergeConsecutiveRunCheck(),
        at(3, MESSAGE), at(4, MESSAGE), at(7, MESSAGE));
  }**/
}

