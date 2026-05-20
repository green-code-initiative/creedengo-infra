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
import javax.annotation.Nonnull;
import org.sonar.check.Rule;
import org.sonar.iac.common.api.checks.CheckContext;
import org.sonar.iac.common.api.checks.IacCheck;
import org.sonar.iac.common.api.checks.InitContext;
import org.sonar.iac.common.yaml.tree.FileTree;
import org.sonar.iac.common.yaml.tree.MappingTree;
import org.sonar.iac.common.yaml.tree.TupleTree;

/**
 * GCI1060 — flag GitHub-Actions {@code runs-on:} values pinned to a moving
 * tag ({@code *-latest}) or using a generic x86 runner when an ARM
 * equivalent exists. See {@code cipinrunnerandarm.md}.
 *
 * <p>Two issues per job at most:</p>
 * <ul>
 *   <li>{@code runs-on: *-latest} → pin to a specific runner version.</li>
 *   <li>{@code runs-on: ubuntu-22.04} (or any non-ARM Linux runner) →
 *       consider the matching ARM runner (e.g. {@code ubuntu-22.04-arm})
 *       when the build supports it.</li>
 * </ul>
 *
 * <p>Jobs with {@code # eco-design:arch=x86} as a {@code if:} expression or
 * inside the {@code runs-on:} string itself opt-out of the ARM hint.</p>
 */
@Rule(key = "GCI1060")
public class CiPinRunnerAndArmCheck implements IacCheck {

    @Override
    public void initialize(InitContext initContext) {

    }
}

