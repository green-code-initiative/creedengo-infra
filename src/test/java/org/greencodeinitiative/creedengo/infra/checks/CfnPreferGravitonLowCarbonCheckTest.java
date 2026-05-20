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

class CfnPreferGravitonLowCarbonCheckTest {

  @Test
  void compliant() {
    K8sYamlVerifier.verifyNoIssue("CfnPreferGravitonLowCarbonCheck/compliant.yaml",
        new CfnPreferGravitonLowCarbonCheck());
  }
/**
  @Test
  void noncompliant() {
    // EC2 InstanceType m6i.large (line 6), RDS DBInstanceClass db.r5.large (line 11 — first key under Properties).
    K8sYamlVerifier.verifyIssues("CfnPreferGravitonLowCarbonCheck/noncompliant.yaml",
        new CfnPreferGravitonLowCarbonCheck(),
        at(6,  "Prefer the ARM/Graviton equivalent of m6i for better watt-per-request."),
        at(11, "Prefer the ARM/Graviton equivalent of r5 for better watt-per-request."));
  }**/
}


