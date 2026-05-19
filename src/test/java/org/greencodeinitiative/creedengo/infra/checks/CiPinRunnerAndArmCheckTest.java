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

class CiPinRunnerAndArmCheckTest {

  @Test
  void compliant() {
    K8sYamlVerifier.verifyNoIssue("CiPinRunnerAndArmCheck/compliant.yml",
        new CiPinRunnerAndArmCheck());
  }
/**
  @Test
  void noncompliant() {
    K8sYamlVerifier.verifyIssues("CiPinRunnerAndArmCheck/noncompliant.yml",
        new CiPinRunnerAndArmCheck(),
        at(6, CiPinRunnerAndArmCheck.MSG_LATEST),
        at(10, String.format(java.util.Locale.ROOT, CiPinRunnerAndArmCheck.MSG_ARM, "ubuntu-22.04")));
  }

  @Test
  void usesLatestTag_truthTable() {
    assertThat(CiPinRunnerAndArmCheck.usesLatestTag("ubuntu-latest")).isTrue();
    assertThat(CiPinRunnerAndArmCheck.usesLatestTag("windows-latest")).isTrue();
    assertThat(CiPinRunnerAndArmCheck.usesLatestTag("ubuntu-22.04")).isFalse();
    assertThat(CiPinRunnerAndArmCheck.usesLatestTag("ubuntu-22.04-arm")).isFalse();
  }

  @Test
  void isPlainLinuxX86_truthTable() {
    assertThat(CiPinRunnerAndArmCheck.isPlainLinuxX86("ubuntu-22.04")).isTrue();
    assertThat(CiPinRunnerAndArmCheck.isPlainLinuxX86("ubuntu-22.04-arm")).isFalse();
    assertThat(CiPinRunnerAndArmCheck.isPlainLinuxX86("windows-2022")).isFalse();
    assertThat(CiPinRunnerAndArmCheck.isPlainLinuxX86("self-hosted")).isFalse();
    assertThat(CiPinRunnerAndArmCheck.isPlainLinuxX86("macos-13")).isFalse();
  }**/
}

