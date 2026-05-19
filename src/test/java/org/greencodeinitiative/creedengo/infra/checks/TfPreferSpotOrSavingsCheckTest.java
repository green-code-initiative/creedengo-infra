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

class TfPreferSpotOrSavingsCheckTest {

  private static final String MSG =
      "Consider mixing Spot / Preemptible / Low-Priority capacity in this pool — fault-tolerant workloads run cheaper and on otherwise-idle datacenter capacity.";
/**
  @Test
  void compliant() {
    TerraformVerifier.verifyNoIssue("TfPreferSpotOrSavingsCheck/compliant.tf",
        new TfPreferSpotOrSavingsCheck());
  }

  @Test
  void noncompliant() {
    TerraformVerifier.verifyIssues("TfPreferSpotOrSavingsCheck/noncompliant.tf",
        new TfPreferSpotOrSavingsCheck(),
        at(1,  MSG),
        at(10, MSG),
        at(15, MSG));
  }**/
}

