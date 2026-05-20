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
import org.sonar.iac.common.api.checks.CheckContext;
import org.sonar.iac.common.api.checks.IacCheck;
import org.sonar.iac.common.api.checks.InitContext;
import org.sonar.iac.docker.tree.api.Body;
import org.sonar.iac.docker.tree.api.DockerImage;
import org.sonar.iac.docker.tree.api.Instruction;
import org.sonar.iac.docker.tree.api.RunInstruction;

/**
 * GCI1032 — flag consecutive {@code RUN} instructions inside the same stage.
 *
 * <p>Each {@code RUN} creates a new layer, which inflates registry metadata
 * and prevents in-layer cleanup (files deleted in a later {@code RUN} still
 * occupy space in the earlier layer — "layer fossilisation"). Merging
 * related shell steps with {@code &&} typically saves 50–300 MB and shortens
 * cold-start pulls — see {@code mergeconsecutiverun.md}.</p>
 *
 * <p>Algorithm: scan each stage's instructions; whenever two
 * {@link RunInstruction}s are adjacent (no other instruction in between), the
 * second one is flagged. Reporting the second occurrence keeps the message
 * actionable ("merge me into the previous RUN") and avoids cascading reports
 * on a long block of consecutive RUNs.</p>
 */
@Rule(key = "GCI1032")
public class MergeConsecutiveRunCheck implements IacCheck {

  private static final String MESSAGE =
      "Merge this RUN with the previous one (using && and \\) to keep layers small and let cleanup happen in the same layer.";

  @Override
  public void initialize(@Nonnull InitContext init) {
    init.register(Body.class, MergeConsecutiveRunCheck::check);
  }

  private static void check(CheckContext ctx, Body body) {
    for (DockerImage image : body.dockerImages()) {
      RunInstruction previous = null;
      for (Instruction instr : image.instructions()) {
        if (instr instanceof RunInstruction run) {
          if (previous != null) {
            ctx.reportIssue(run, MESSAGE);
          }
          previous = run;
        } else {
          previous = null;
        }
      }
    }
  }
}
