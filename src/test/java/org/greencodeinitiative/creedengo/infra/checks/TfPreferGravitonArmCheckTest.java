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

import org.junit.jupiter.api.Test;

class TfPreferGravitonArmCheckTest {

  @Test
  void compliant() {
    TerraformVerifier.verifyNoIssue("TfPreferGravitonArmCheck/compliant.tf", new TfPreferGravitonArmCheck());
  }

  @Test
  void noncompliant() {
    TerraformVerifier.verifyIssues("TfPreferGravitonArmCheck/noncompliant.tf",
        new TfPreferGravitonArmCheck(),
        at(2,  "Prefer the ARM/Graviton equivalent of m6i (e.g. m7g) for better watt-per-request."),
        at(7,  "Prefer the ARM/Graviton equivalent of c5 (e.g. c6g/c7g) for better watt-per-request."),
        at(11, "Prefer the ARM/Graviton equivalent of r6i (e.g. r7g) for better watt-per-request."));
  }

  @Test
  void parseFamily_extractsPrefix() {
    assertThat(TfPreferGravitonArmCheck.parseFamily("m6i.large")).isEqualTo("m6i");
    assertThat(TfPreferGravitonArmCheck.parseFamily("t4g.medium")).isEqualTo("t4g");
    assertThat(TfPreferGravitonArmCheck.parseFamily("c7g")).isEqualTo("c7g");
    assertThat(TfPreferGravitonArmCheck.parseFamily("")).isEmpty();
    assertThat(TfPreferGravitonArmCheck.parseFamily(null)).isEmpty();
  }
}

