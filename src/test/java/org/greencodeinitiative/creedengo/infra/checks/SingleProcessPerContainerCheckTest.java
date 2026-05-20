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

class SingleProcessPerContainerCheckTest {

  private final IacCheck check = new SingleProcessPerContainerCheck();

  private static final String MSG_SUPERVISOR =
      "Avoid process supervisors in a container — split the workloads into separate images so each can be scaled independently.";
  private static final String MSG_BACKGROUND =
      "CMD/ENTRYPOINT should launch a single foreground process — trailing `&` or `; <cmd> &` defeats liveness probes and HPA scaling.";
/**
  @Test
  void shouldNotRaiseOnSingleForegroundProcess() {
    DockerVerifier.verifyNoIssue("SingleProcessPerContainerCheck/compliant.Dockerfile", check);
  }

  @Test
  void shouldRaiseOnSupervisorInvocation() {
    DockerVerifier.verifyIssues(
        "SingleProcessPerContainerCheck/supervisor.Dockerfile", check,
        DockerVerifier.ExpectedIssue.at(4, MSG_SUPERVISOR));
  }

  @Test
  void shouldRaiseOnBackgroundProcessChaining() {
    DockerVerifier.verifyIssues(
        "SingleProcessPerContainerCheck/background.Dockerfile", check,
        DockerVerifier.ExpectedIssue.at(3, MSG_BACKGROUND));
  }**/
}

