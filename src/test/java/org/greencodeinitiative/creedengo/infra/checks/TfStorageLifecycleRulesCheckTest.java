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

class TfStorageLifecycleRulesCheckTest {

  private static final String MESSAGE =
      "Declare a lifecycle policy on this bucket so cold objects move to archive tiers or expire.";

  @Test
  void compliant() {
    // aws_s3_bucket paired with a sibling aws_s3_bucket_lifecycle_configuration;
    // google_storage_bucket carries its own nested lifecycle_rule block.
    TerraformVerifier.verifyNoIssue("TfStorageLifecycleRulesCheck/compliant.tf",
        new TfStorageLifecycleRulesCheck());
  }

  @Test
  void noncompliant() {
    // aws_s3_bucket (line 1) and google_storage_bucket (line 5) declared without any lifecycle.
    TerraformVerifier.verifyIssues("TfStorageLifecycleRulesCheck/noncompliant.tf",
        new TfStorageLifecycleRulesCheck(),
        at(1, MESSAGE),
        at(5, MESSAGE));
  }
}

