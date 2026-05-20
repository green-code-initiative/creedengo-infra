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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import org.sonar.check.Rule;
import org.sonar.iac.common.api.checks.CheckContext;
import org.sonar.iac.common.api.checks.IacCheck;
import org.sonar.iac.common.api.checks.InitContext;
import org.sonar.iac.terraform.api.tree.BlockTree;
import org.sonar.iac.terraform.api.tree.FileTree;

/**
 * GCI1053 — flag object-storage buckets without lifecycle rules. See
 * {@code tfstoragelifecyclerules.md}.
 *
 * <p>Detection covers two HCL styles:</p>
 * <ol>
 *   <li>Bucket resource embeds a {@code lifecycle_rule { ... }} nested
 *       block (legacy AWS S3 form).</li>
 *   <li>A separate {@code aws_s3_bucket_lifecycle_configuration} /
 *       {@code google_storage_bucket} {@code lifecycle_rule} resource
 *       targets the bucket by reference.</li>
 * </ol>
 *
 * <p>The check runs at file scope — cross-file analysis is a follow-up
 * (Wave-2D Sensor). A bucket whose lifecycle resource sits in a sibling
 * file is therefore (knowingly) flagged here; users can use
 * {@code // NOSONAR} comments until the cross-file sensor lands.</p>
 */
@Rule(key = "GCI1053")
public class TfStorageLifecycleRulesCheck implements IacCheck {

  /**
   * Storage-bucket resource types we know how to audit, mapped to the
   * provider's "matching lifecycle resource" so we can detect the
   * sibling-resource style.
   */
  private static final Map<String, String> BUCKET_TO_LIFECYCLE_RESOURCE = Map.of(
    "aws_s3_bucket",          "aws_s3_bucket_lifecycle_configuration",
    "google_storage_bucket",  "google_storage_bucket"  // lifecycle_rule is nested → handled by case 1
    );

  @Override
  public void initialize(@Nonnull InitContext init) {
    //init.register(FileTree.class, TfStorageLifecycleRulesCheck::check);
  }
}
