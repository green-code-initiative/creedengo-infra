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

import org.sonar.check.Rule;
import org.sonar.iac.common.api.checks.IacCheck;
import org.sonar.iac.common.api.checks.InitContext;

/**
 * Metadata-only carrier for rule GCI1028 ({@code DeleteUnnecessaryFiles}).
 *
 * <p>The actual detection lives in
 * {@link org.greencodeinitiative.creedengo.infra.DockerignoreSensor} because
 * the rule audits a sibling {@code .dockerignore} file that is <em>not</em>
 * part of the sonar-iac Docker AST. This {@link IacCheck} stays registered in
 * {@link org.greencodeinitiative.creedengo.infra.InfraDockerCheckRegistrar} so
 * that {@code RuleMetadataLoader} loads the JSON/HTML rule definition; it
 * intentionally registers no AST visitor.</p>
 */
@Rule(key = "GCI1028")
public class DeleteUnnecessaryFilesCheck implements IacCheck {

  @Override
  public void initialize(@javax.annotation.Nonnull InitContext init) {
    // No AST visitor — detection is performed by DockerignoreSensor.
  }
}
