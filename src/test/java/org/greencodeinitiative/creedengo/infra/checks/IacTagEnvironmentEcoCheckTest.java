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

import static org.greencodeinitiative.creedengo.infra.checks.DockerVerifier.ExpectedIssue.at;

import org.junit.jupiter.api.Test;

class IacTagEnvironmentEcoCheckTest {

  @Test
  void compliant() {
    K8sYamlVerifier.verifyNoIssue("IacTagEnvironmentEcoCheck/compliant.yml",
        new IacTagEnvironmentEcoCheck());
  }

  @Test
  void noncompliant() {
    K8sYamlVerifier.verifyIssues("IacTagEnvironmentEcoCheck/noncompliant.yml",
        new IacTagEnvironmentEcoCheck(),
        at(4,  "Missing required labels [environment, owner] on this Deployment — add them under metadata.labels for cost/carbon attribution."),
        at(12, "Missing required tags [environment, owner] on CloudFormation resource 'Web' — add them under Properties.Tags for cost/carbon attribution."));
  }
}

