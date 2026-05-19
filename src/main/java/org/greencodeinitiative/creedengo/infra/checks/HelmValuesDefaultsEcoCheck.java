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
 * Metadata-only carrier for rule GCI1049 ({@code HelmValuesDefaultsEco}).
 *
 * <p>Detection lives in
 * {@link org.greencodeinitiative.creedengo.infra.HelmValuesEcoSensor}. The
 * sensor audits Helm chart {@code values.yaml} defaults (replicaCount,
 * resources, imagePullPolicy, autoscaling.enabled) for eco-design
 * anti-patterns, with opt-out via {@code Chart.yaml} annotation
 * {@code creedengo.io/eco-design: quorum}. This {@link IacCheck} stays
 * registered for metadata loading only.</p>
 */
@Rule(key = "GCI1049")
public class HelmValuesDefaultsEcoCheck implements IacCheck {

  @Override
  public void initialize(@javax.annotation.Nonnull InitContext init) {
    // No AST visitor — detection is performed by HelmValuesEcoSensor.
  }
}
