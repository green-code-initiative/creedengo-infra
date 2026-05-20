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

class ImagePullPolicyNotAlwaysCheckTest {

  private static final String MESSAGE =
      "Use imagePullPolicy: IfNotPresent on pinned/tagged images to remove redundant registry round-trips.";

  @Test
  void compliant() {
    // - :latest with Always: not flagged here (covered by GCI1031)
    // - sha256 digest + IfNotPresent: OK
    // - explicit tag with no pull policy: OK
    K8sYamlVerifier.verifyNoIssue("ImagePullPolicyNotAlwaysCheck/compliant.yaml",
        new ImagePullPolicyNotAlwaysCheck());
  }
/**
  @Test
  void noncompliant() {
    K8sYamlVerifier.verifyIssues("ImagePullPolicyNotAlwaysCheck/noncompliant.yaml",
        new ImagePullPolicyNotAlwaysCheck(),
        at(7, MESSAGE), at(10, MESSAGE), at(13, MESSAGE));
  }**/
}

