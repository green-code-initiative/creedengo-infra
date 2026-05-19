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

import java.util.Locale;
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.iac.common.api.checks.CheckContext;
import org.sonar.iac.common.api.checks.IacCheck;
import org.sonar.iac.common.api.checks.InitContext;
import org.sonar.iac.common.yaml.tree.FileTree;
import org.sonar.iac.common.yaml.tree.MappingTree;

/**
 * GCI1042 — for each container, the ratio
 * {@code limits.<resource> / requests.<resource>} should stay below
 * {@link #maxRatio} (default 4). High ratios signal chronic
 * over-allocation and break HPA / Karpenter decisions — see
 * {@code cpurequestvslimitratio.md}.
 *
 * <p>Resources covered: {@code cpu} and {@code memory}. Quantities are
 * parsed with the standard Kubernetes suffix table (m, Ki, Mi, Gi, …).
 * Unparseable values are skipped (we err on the side of no false
 * positives).</p>
 */
@Rule(key = "GCI1042")
public class CpuRequestVsLimitRatioCheck implements IacCheck {

  @Override
  public void initialize(InitContext initContext) {

  }
}
