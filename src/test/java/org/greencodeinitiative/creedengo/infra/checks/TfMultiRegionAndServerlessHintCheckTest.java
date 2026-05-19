/*
 * Creedengo Infra plugin - Provides rules to reduce the environmental footprint of your infra as code
 * Copyright  2025 Green Code Initiative (https://green-code-initiative.org)
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

/**
 * Placeholder test class for {@link TfMultiRegionAndServerlessHintCheck}.
 *
 * <p>Mirrors the convention of the other {@code Tf*CheckTest} classes in this
 * package: real assertions are gated on the wiring of the
 * {@code TerraformVerifier} harness (tracked in {@code doc/IMPORT-NOTES.md}
 * 7). Until then the check is exercised end-to-end via the
 * {@code InfraTerraformSensor} integration tests and the {@code .tf} fixtures
 * under {@code src/test/resources/checks/TfMultiRegionAndServerlessHintCheck/}.</p>
 */
class TfMultiRegionAndServerlessHintCheckTest {

  @Test
  void instantiation_compiles() {
    // Smoke check: the class must be instantiable and declare key GCI1066.
    TfMultiRegionAndServerlessHintCheck check = new TfMultiRegionAndServerlessHintCheck();
    org.junit.jupiter.api.Assertions.assertNotNull(check);
    org.sonar.check.Rule rule = check.getClass().getAnnotation(org.sonar.check.Rule.class);
    org.junit.jupiter.api.Assertions.assertNotNull(rule);
    org.junit.jupiter.api.Assertions.assertEquals("GCI1066", rule.key());
  }

  /*
  @Test
  void multi_region_aliased_providers() {
    TerraformVerifier.verifyIssues(
        "TfMultiRegionAndServerlessHintCheck/multi_region_aliased_providers.tf",
        new TfMultiRegionAndServerlessHintCheck(),
        at(8, "Multi-region deployment detected (provider 'aws', regions: [eu-west-3, us-east-1])."));
  }

  @Test
  void always_on_compute() {
    TerraformVerifier.verifyIssues(
        "TfMultiRegionAndServerlessHintCheck/always_on_compute.tf",
        new TfMultiRegionAndServerlessHintCheck(),
        at(3,  "Resource 'aws_instance' ('cron_runner') is always-on. Consider a serverless alternative (AWS Lambda or Fargate)."),
        at(8,  "Resource 'azurerm_linux_virtual_machine' ('worker') is always-on. Consider a serverless alternative (Azure Functions or Container Apps)."),
        at(15, "Resource 'google_compute_instance' ('batch') is always-on. Consider a serverless alternative (Cloud Run or Cloud Functions)."));
  }

  @Test
  void single_region_no_issue() {
    TerraformVerifier.verifyNoIssue(
        "TfMultiRegionAndServerlessHintCheck/single_region_ok.tf",
        new TfMultiRegionAndServerlessHintCheck());
  }
  */
}

