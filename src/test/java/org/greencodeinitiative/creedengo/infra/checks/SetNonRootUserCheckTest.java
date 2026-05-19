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

class SetNonRootUserCheckTest {
  private final IacCheck check = new SetNonRootUserCheck();
  private static final String MSG_MISSING =
      "Declare an explicit non-root USER in the final stage to avoid CVE-driven emergency redeploys.";
  private static final String MSG_ROOT =
      "Avoid USER root / USER 0 in the final stage — switch to a dedicated low-privilege account.";

  @Test
  void shouldNotRaiseWhenNonRootUserDeclared() {
    DockerVerifier.verifyNoIssue("SetNonRootUserCheck/compliant.Dockerfile", check);
  }

  @Test
  void shouldRaiseWhenNoUserInstruction() {
    DockerVerifier.verifyIssues("SetNonRootUserCheck/noncompliant_missing.Dockerfile", check,
        DockerVerifier.ExpectedIssue.at(1, MSG_MISSING));
  }

  @Test
  void shouldRaiseWhenUserIsRoot() {
    DockerVerifier.verifyIssues("SetNonRootUserCheck/noncompliant_root.Dockerfile", check,
        DockerVerifier.ExpectedIssue.at(2, MSG_ROOT));
  }
}

