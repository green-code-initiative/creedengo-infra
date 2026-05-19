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
import static org.greencodeinitiative.creedengo.infra.checks.DockerVerifier.ExpectedIssue.at;

import java.util.Locale;
import org.junit.jupiter.api.Test;

class CiPinActionsCheckTest {

  @Test
  void compliant() {
    K8sYamlVerifier.verifyNoIssue("CiPinActionsCheck/compliant.yml",
        new CiPinActionsCheck());
  }

  @Test
  void noncompliant() {
    K8sYamlVerifier.verifyIssues("CiPinActionsCheck/noncompliant.yml",
        new CiPinActionsCheck(),
        at(8, String.format(Locale.ROOT, CiPinActionsCheck.MSG, "tj-actions/changed-files", "v44")),
        at(9, String.format(Locale.ROOT, CiPinActionsCheck.MSG, "some-org/deploy", "main")),
        at(10, String.format(Locale.ROOT, CiPinActionsCheck.MSG, "another-org/release", "release-1.2")));
  }

  @Test
  void isExemptReference_truthTable() {
    assertThat(CiPinActionsCheck.isExemptReference("actions/checkout@v4")).isTrue();
    assertThat(CiPinActionsCheck.isExemptReference("./.github/actions/local")).isTrue();
    assertThat(CiPinActionsCheck.isExemptReference("docker://alpine:3.20")).isTrue();
    assertThat(CiPinActionsCheck.isExemptReference("tj-actions/changed-files@v44")).isFalse();
  }

  @Test
  void refOf_truthTable() {
    assertThat(CiPinActionsCheck.refOf("owner/repo@v1")).isEqualTo("v1");
    assertThat(CiPinActionsCheck.refOf("owner/repo@2f7c5bfce28377bc069a65ba478de0a74aa0ca32"))
        .isEqualTo("2f7c5bfce28377bc069a65ba478de0a74aa0ca32");
    assertThat(CiPinActionsCheck.refOf("owner/repo")).isNull();
    assertThat(CiPinActionsCheck.refOf("owner/repo@")).isNull();
  }
}

