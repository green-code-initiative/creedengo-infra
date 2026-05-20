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

import javax.annotation.Nonnull;
import org.sonar.check.Rule;
import org.sonar.iac.common.api.checks.IacCheck;
import org.sonar.iac.common.api.checks.InitContext;
import org.sonar.iac.docker.symbols.ArgumentResolution;
import org.sonar.iac.docker.tree.api.FromInstruction;

/**
 * GCI1031 — Do not use the {@code latest} tag (or implicit tag) on base images.
 *
 * <p>Reference template for Wave-2B Docker rules (see
 * {@code creedengo-infra/avoidlatesttag.md} and the per-rule spec under
 * {@code creedengo-rules-specifications/src/main/rules/GCI1031/}).</p>
 *
 * <p>Detection rules:</p>
 * <ul>
 *   <li>{@code FROM image} with no tag → implicit {@code :latest} → flag.</li>
 *   <li>{@code FROM image:latest} → flag.</li>
 *   <li>{@code FROM image@sha256:...} digest pin → OK, regardless of any tag.</li>
 *   <li>{@code FROM scratch} → OK (no tag concept).</li>
 *   <li>Unresolved argument (e.g. {@code FROM $BASE}) → skipped to avoid false
 *       positives until a {@code ProjectContext} resolves build args.</li>
 * </ul>
 */
@Rule(key = "GCI1031")
public class AvoidLatestTagCheck implements IacCheck {

    @Override
    public void initialize(InitContext initContext) {

    }
}
