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
import java.util.Set;
import javax.annotation.Nonnull;
import org.sonar.check.Rule;
import org.sonar.iac.common.api.checks.IacCheck;
import org.sonar.iac.common.api.checks.InitContext;
import org.sonar.iac.docker.tree.api.Body;
import org.sonar.iac.docker.tree.api.DockerImage;
import org.sonar.iac.docker.tree.api.RunInstruction;

/**
 * GCI1025 — Use a Docker multi-stage build when build tooling is invoked.
 *
 * <p>Heuristic: when a Dockerfile has a single {@code FROM} stage AND that stage
 * runs build tooling (maven/gradle/npm/go/cargo/rustc/pip wheel/make/javac),
 * the build toolchain ends up baked into the runtime image. The fix is to
 * split the file into a builder stage + a slim runtime stage. False positives
 * are possible (e.g. ad-hoc utility images) — they can be silenced with a
 * Sonar NOSONAR or by switching to {@code FROM scratch}.</p>
 */
@Rule(key = "GCI1025")
public class MultistageCheck implements IacCheck {

  private static final String MESSAGE =
      "Use a multi-stage build: separate the build toolchain from the runtime image to avoid shipping compilers and package managers.";

  /** Tokens that strongly suggest a build / compile step. */
  private static final Set<String> BUILD_TOKENS = Set.of(
      "mvn", "gradle", "./gradlew", "gradlew",
      "npm run build", "yarn build", "pnpm build",
      "go build", "cargo build", "rustc",
      "javac", "make ", "make\n",
      "pip wheel", "python setup.py build",
      "tsc ", "webpack", "vite build");

  @Override
  public void initialize(@Nonnull InitContext init) {
    init.register(Body.class, (ctx, body) -> {
      List<DockerImage> images = body.dockerImages();
      if (images.size() != 1) {
        return;
      }
      DockerImage only = images.get(0);
      boolean runsBuild = only.instructions().stream()
          .filter(RunInstruction.class::isInstance)
          .map(RunInstruction.class::cast)
          .map(DockerCheckUtils::joinCommandText)
          .anyMatch(MultistageCheck::looksLikeBuild);
      if (runsBuild) {
        ctx.reportIssue(only.from(), MESSAGE);
      }
    });
  }

  private static boolean looksLikeBuild(String command) {
    if (command == null || command.isBlank()) {
      return false;
    }
    String lower = command.toLowerCase();
    return BUILD_TOKENS.stream().anyMatch(lower::contains);
  }
}
