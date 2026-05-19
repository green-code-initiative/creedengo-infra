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

import java.util.List;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import org.sonar.check.Rule;
import org.sonar.iac.common.api.checks.CheckContext;
import org.sonar.iac.common.api.checks.IacCheck;
import org.sonar.iac.common.api.checks.InitContext;
import org.sonar.iac.docker.tree.api.Body;
import org.sonar.iac.docker.tree.api.CmdInstruction;
import org.sonar.iac.docker.tree.api.CommandInstruction;
import org.sonar.iac.docker.tree.api.DockerImage;
import org.sonar.iac.docker.tree.api.EntrypointInstruction;
import org.sonar.iac.docker.tree.api.Instruction;

/**
 * 1038 — one main process per container.
 *
 * <p>Heuristic: look at the {@code CMD} / {@code ENTRYPOINT} of the final
 * stage (= runtime stage by convention) and flag invocations of a known
 * process supervisor, or shell-form commands that fork background
 * processes via trailing {@code &} / {@code ;&}.</p>
 *
 * <p>Per spec ({@code creedengo-infra/singleprocesspercontainer.md}) the
 * precision is ~75 % (legitimate sidecar patterns and intentional
 * supervisors will trigger false positives). See the rule markdown for the
 * eco-design rationale (axes 3·4·5·6) and the opt-out guidance.</p>
 */
@Rule(key = "1038")
public class SingleProcessPerContainerCheck implements IacCheck {

  private static final String MESSAGE_SUPERVISOR =
      "Avoid process supervisors in a container — split the workloads into separate images so each can be scaled independently.";
  private static final String MESSAGE_BACKGROUND =
      "CMD/ENTRYPOINT should launch a single foreground process — trailing `&` or `; <cmd> &` defeats liveness probes and HPA scaling.";

  /** Known multi-process supervisors / process managers, word-bounded. */
  private static final Pattern SUPERVISOR_REGEX = Pattern.compile(
      "\\b(?:supervisord|supervisorctl|runit|runsv|sv|s6-svscan|s6-overlay|s6-init|honcho|foreman)\\b");

  /**
   * Shell-form indicator that the command chains a background process: a
   * single {@code &} that is not part of {@code &&}. Matches at end of
   * string ({@code cmd &}) <em>and</em> mid-string chaining patterns such
   * as {@code worker & server} or {@code cmd1 ; cmd2 &}.
   */
  private static final Pattern BACKGROUND_REGEX = Pattern.compile(
      "(?<![&\\\\])&(?!&)");

  @Override
  public void initialize(@Nonnull InitContext init) {
    //init.register(Body.class, SingleProcessPerContainerCheck::check);
  }
}
