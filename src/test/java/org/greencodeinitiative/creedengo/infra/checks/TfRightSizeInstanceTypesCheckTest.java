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

class TfRightSizeInstanceTypesCheckTest {

  @Test
  void compliant() {
    TerraformVerifier.verifyNoIssue("TfRightSizeInstanceTypesCheck/compliant.tf",
        new TfRightSizeInstanceTypesCheck());
  }

  @Test
  void noncompliant() {
    TerraformVerifier.verifyIssues("TfRightSizeInstanceTypesCheck/noncompliant.tf",
        new TfRightSizeInstanceTypesCheck(),
        at(2,  "Instance size 'm6i.2xlarge' looks oversized — document the capacity plan via a 'sizing-rationale' tag or enable autoscaling."),
        at(7,  "Instance size 'db.r6i.4xlarge' looks oversized — document the capacity plan via a 'sizing-rationale' tag or enable autoscaling."),
        at(11, "Instance size 'n2-standard-16' looks oversized — document the capacity plan via a 'sizing-rationale' tag or enable autoscaling."),
        at(15, "Instance size 'Standard_D16s_v3' looks oversized — document the capacity plan via a 'sizing-rationale' tag or enable autoscaling."));
  }

  @Test
  void isOversized_awsSizes() {
    assertThat(TfRightSizeInstanceTypesCheck.isOversized("m6i.2xlarge")).isTrue();
    assertThat(TfRightSizeInstanceTypesCheck.isOversized("c5.12xlarge")).isTrue();
    assertThat(TfRightSizeInstanceTypesCheck.isOversized("m6i.metal")).isTrue();
    assertThat(TfRightSizeInstanceTypesCheck.isOversized("m6i.large")).isFalse();
    assertThat(TfRightSizeInstanceTypesCheck.isOversized("t4g.small")).isFalse();
    assertThat(TfRightSizeInstanceTypesCheck.isOversized("m6i.xlarge")).isFalse();
  }

  @Test
  void isOversized_gcpAndAzure() {
    assertThat(TfRightSizeInstanceTypesCheck.isOversized("n2-standard-16")).isTrue();
    assertThat(TfRightSizeInstanceTypesCheck.isOversized("e2-standard-2")).isFalse();
    assertThat(TfRightSizeInstanceTypesCheck.isOversized("Standard_D16s_v3")).isTrue();
    assertThat(TfRightSizeInstanceTypesCheck.isOversized("Standard_D2_v3")).isFalse();
    assertThat(TfRightSizeInstanceTypesCheck.isOversized("")).isFalse();
    assertThat(TfRightSizeInstanceTypesCheck.isOversized(null)).isFalse();
  }
}

