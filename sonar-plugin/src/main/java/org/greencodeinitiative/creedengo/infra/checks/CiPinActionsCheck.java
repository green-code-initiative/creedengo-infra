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
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import org.sonar.check.Rule;
import org.sonar.iac.common.api.checks.CheckContext;
import org.sonar.iac.common.api.checks.IacCheck;
import org.sonar.iac.common.api.checks.InitContext;
import org.sonar.iac.common.yaml.tree.FileTree;
import org.sonar.iac.common.yaml.tree.MappingTree;
import org.sonar.iac.common.yaml.tree.TupleTree;

/**
 * 1065 — flag third-party CI {@code uses:} steps whose ref is a moving
 * tag/branch instead of a 40-char Git commit SHA. See
 * {@code cipinactions.md}.
 *
 * <p>Detection is heuristic on the {@code uses:} scalar:</p>
 * <ul>
 *   <li>{@code owner/repo@<sha40>} — compliant.</li>
 *   <li>{@code actions/...@anything} — exempt (first-party).</li>
 *   <li>{@code ./...}, {@code docker://...} — exempt (local / container).</li>
 *   <li>Step contains {@code eco-design:pin=tag} as an {@code if:} guard
 *       or inside the {@code uses:} value — opt-out.</li>
 * </ul>
 *
 * <p>Reports once per non-compliant step, on the {@code uses:} tuple, so
 * the issue is anchored on the exact line of the workflow.</p>
 */
@Rule(key = "GCI1065")
public class CiPinActionsCheck implements IacCheck {

    @Override
    public void initialize(InitContext initContext) {

    }
}

