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

class TfEnableAutoscalingGroupCheckTest {

  private static final String MSG_AWS =
      "Replace this scaled-out aws_instance by an autoscaling group / MIG / VMSS so capacity tracks demand instead of paying for peak 24/7.";
  private static final String MSG_GCP =
      "Replace this scaled-out google_compute_instance by an autoscaling group / MIG / VMSS so capacity tracks demand instead of paying for peak 24/7.";
  private static final String MSG_AZURE =
      "Replace this scaled-out azurerm_linux_virtual_machine by an autoscaling group / MIG / VMSS so capacity tracks demand instead of paying for peak 24/7.";
/**
  @Test
  void compliant() {
    TerraformVerifier.verifyNoIssue("TfEnableAutoscalingGroupCheck/compliant.tf",
        new TfEnableAutoscalingGroupCheck());
  }

  @Test
  void noncompliant() {
    TerraformVerifier.verifyIssues("TfEnableAutoscalingGroupCheck/noncompliant.tf",
        new TfEnableAutoscalingGroupCheck(),
        at(1,  MSG_AWS),
        at(7,  MSG_GCP),
        at(12, MSG_AZURE));
  }**/
}

