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

class TfAvoidAlwaysOnResourcesCheckTest {
/**
  @Test
  void compliant() {
    TerraformVerifier.verifyNoIssue("TfAvoidAlwaysOnResourcesCheck/compliant.tf",
        new TfAvoidAlwaysOnResourcesCheck());
  }

  @Test
  void noncompliant() {
    TerraformVerifier.verifyIssues("TfAvoidAlwaysOnResourcesCheck/noncompliant.tf",
        new TfAvoidAlwaysOnResourcesCheck(),
        at(1,  "Non-prod resource (environment=staging) without a start/stop schedule — wire an autoscaling schedule, kube-green or AWS Instance Scheduler."),
        at(10, "Non-prod resource (environment=dev) without a start/stop schedule — wire an autoscaling schedule, kube-green or AWS Instance Scheduler."),
        at(17, "Non-prod resource (environment=qa) without a start/stop schedule — wire an autoscaling schedule, kube-green or AWS Instance Scheduler."));
  }

  @Test
  void isNonProd_classifiesCommonAliases() {
    assertThat(TfAvoidAlwaysOnResourcesCheck.isNonProd("dev")).isTrue();
    assertThat(TfAvoidAlwaysOnResourcesCheck.isNonProd("Staging")).isTrue();
    assertThat(TfAvoidAlwaysOnResourcesCheck.isNonProd(" preprod ")).isTrue();
    assertThat(TfAvoidAlwaysOnResourcesCheck.isNonProd("prod")).isFalse();
    assertThat(TfAvoidAlwaysOnResourcesCheck.isNonProd("production")).isFalse();
    assertThat(TfAvoidAlwaysOnResourcesCheck.isNonProd(null)).isFalse();
    assertThat(TfAvoidAlwaysOnResourcesCheck.isNonProd("")).isFalse();
  }**/
}

