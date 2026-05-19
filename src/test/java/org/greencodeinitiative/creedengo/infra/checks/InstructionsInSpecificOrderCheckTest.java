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

class InstructionsInSpecificOrderCheckTest {

  private final IacCheck check = new InstructionsInSpecificOrderCheck();

  private static final String MSG_COPY =
      "Move this COPY of sources after the dependency-install RUN — a wide COPY before it busts the cache on every code change.";
  private static final String MSG_WORKDIR =
      "Declare WORKDIR before the first COPY/RUN — declaring it late forces the working directory to be created implicitly by the previous instructions.";

  @Test
  void shouldNotRaiseOnWellOrderedDockerfile() {
    DockerVerifier.verifyNoIssue("InstructionsInSpecificOrderCheck/compliant.Dockerfile", check);
  }

  @Test
  void shouldRaiseOnWideCopyBeforeInstallAndLateWorkdir() {
    // noncompliant.Dockerfile:
    //   1 FROM ...
    //   2 COPY . .            <- wide copy before install     (sub-rule 1)
    //   3 RUN npm install
    //   4 COPY package*.json ./
    //   5 WORKDIR /app        <- workdir after COPY/RUN       (sub-rule 2)
    //   6 CMD ...
    DockerVerifier.verifyIssues(
        "InstructionsInSpecificOrderCheck/noncompliant.Dockerfile", check,
        DockerVerifier.ExpectedIssue.at(2, MSG_COPY),
        DockerVerifier.ExpectedIssue.at(5, MSG_WORKDIR));
  }

  @Test
  void shouldHandleMultistageWithoutFalsePositives() {
    // In multistage.Dockerfile the builder stage is well-ordered
    // (WORKDIR first, deps then sources) and the runtime stage only
    // contains a `COPY --from=builder ...` (not a wide local COPY,
    // and no install RUN follows it), so nothing should be raised.
    DockerVerifier.verifyNoIssue("InstructionsInSpecificOrderCheck/multistage.Dockerfile", check);
  }
}

