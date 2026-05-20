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
 * <strong>Stub</strong> for rule GCI1045 — prefervpaorrightsizing.
 * <p>
 * Generated as part of the GCI1025–GCI1064 batch (see creedengo-infra/prefervpaorrightsizing.md). The detection
 * logic still needs to be implemented against the kubernetes sonar-iac AST.
 * Until then the check is registered so SonarQube exposes the rule metadata
 * and the quality profile activation works.
 */
@Rule(key = "GCI1045")
public class PreferVpaOrRightsizingCheck implements IacCheck {

  @Override
  public void initialize(@javax.annotation.Nonnull InitContext init) {
    // TODO: register AST visitors against sonar-iac kubernetes tree API.
  }
}
