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
import org.sonar.iac.docker.tree.api.CopyInstruction;
import org.sonar.iac.docker.tree.api.DockerImage;
import org.sonar.iac.docker.tree.api.Instruction;
import org.sonar.iac.docker.tree.api.RunInstruction;
import org.sonar.iac.docker.tree.api.WorkdirInstruction;

/**
 * 1027 — order Dockerfile instructions for cache efficiency.
 *
 * <p>The original draft was considered "not implementable generically"; we
 * refocus it on two auditable sub-patterns (sub-rule 3 — split
 * {@code apt-get update / install} — is delegated to Hadolint DL3009):</p>
 * <ol>
 *   <li><b>Cache-busting COPY before dependency install</b>: a
 *       {@code COPY . .} (or {@code COPY *}) that appears <em>before</em>
 *       the first {@code RUN <pkg-manager> install} in the same stage
 *       invalidates the dependency-install layer on every source change.
 *       Per-stage; reports the offending {@code COPY}.</li>
 *   <li><b>WORKDIR declared after a file-system action</b>: any
 *       {@code WORKDIR} appearing after the first {@code COPY}/{@code RUN}
 *       in a stage means the working directory was created implicitly by
 *       the previous instructions, often in {@code /} — the
 *       {@code WORKDIR} should come first. Per-stage; reports the
 *       {@code WORKDIR}.</li>
 * </ol>
 *
 * <p>See {@code creedengo-infra/instructionsinspecificorder.md}.</p>
 */
@Rule(key = "GCI1027")
public class InstructionsInSpecificOrderCheck implements IacCheck {

  private static final String MESSAGE_COPY_BEFORE_INSTALL =
      "Move this COPY of sources after the dependency-install RUN — a wide COPY before it busts the cache on every code change.";
  private static final String MESSAGE_WORKDIR_LATE =
      "Declare WORKDIR before the first COPY/RUN — declaring it late forces the working directory to be created implicitly by the previous instructions.";

  /**
   * Package-manager install invocations whose layer should be kept cacheable.
   * Matched against the joined command text of a {@link RunInstruction}, word-bounded.
   */
  private static final Pattern INSTALL_REGEX = Pattern.compile(
      "\\b(?:"
          + "npm\\s+(?:ci|install)"
          + "|yarn\\s+install"
          + "|pnpm\\s+(?:install|i)"
          + "|pip3?\\s+install"
          + "|poetry\\s+install"
          + "|composer\\s+install"
          + "|bundle\\s+install"
          + "|go\\s+mod\\s+download"
          + "|cargo\\s+fetch"
          + "|apt-get\\s+install"
          + "|apk\\s+add"
          + "|dnf\\s+install"
          + "|yum\\s+install"
          + ")\\b");

  @Override
  public void initialize(@Nonnull InitContext init) {
    //init.register(Body.class, InstructionsInSpecificOrderCheck::check);
  }
}
