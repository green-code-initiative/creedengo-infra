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
import org.sonar.iac.common.api.checks.CheckContext;
import org.sonar.iac.common.api.checks.IacCheck;
import org.sonar.iac.common.api.checks.InitContext;
import org.sonar.iac.docker.tree.api.Body;
import org.sonar.iac.docker.tree.api.DockerImage;
import org.sonar.iac.docker.tree.api.RunInstruction;

/**
 * GCI1036 — flag installs of build toolchains in the final stage of a
 * Dockerfile. Compilers, headers and SDKs are needed at build time but
 * useless at runtime: keeping them in the final image adds 100 MB–1 GB,
 * slows cold starts and expands the CVE surface. Complements
 * {@link MultistageCheck} (GCI1025) — see {@code nobuildtoolsinruntime.md}.
 *
 * <p>Algorithm: walk the file's stages, target only the <strong>last</strong>
 * {@link DockerImage} (= runtime stage by convention), and report any
 * {@link RunInstruction} whose joined command text installs at least one
 * package from {@link #BUILD_PACKAGES} via apt / apk / dnf / yum / pip /
 * npm / yarn.</p>
 */
@Rule(key = "GCI1036")
public class NoBuildToolsInRuntimeCheck implements IacCheck {

  private static final String MESSAGE =
      "Move build tools to a separate builder stage — keeping compilers/SDKs in the runtime image inflates size and CVE surface.";

  /**
   * Build-only packages whose presence in the runtime stage is a strong
   * eco-design smell. Lower-case, plain tokens (no shell quoting).
   */
  static final Set<String> BUILD_PACKAGES = Set.of(
      "gcc", "g++", "clang", "make", "cmake", "automake", "autoconf",
      "build-essential", "musl-dev", "alpine-sdk", "libc-dev", "linux-headers",
      "git", "subversion",
      "maven", "mvn", "gradle",
      "npm", "yarn", "pnpm",
      "pip", "pip3", "poetry", "pipenv",
      "python-dev", "python3-dev",
      "openjdk-17-jdk", "openjdk-11-jdk", "openjdk-8-jdk",
      "kernel-headers");

  /**
   * Package-manager invocation prefixes. Matching any of these greatly
   * reduces false positives (e.g. echo of the word "make").
   */
  private static final List<String> INSTALL_HINTS = List.of(
      "apt-get install", "apt install",
      "apk add", "apk --no-cache add",
      "dnf install", "yum install", "microdnf install",
      "pip install", "pip3 install",
      "npm install", "npm i ", "npm i\n", "yarn add", "pnpm add",
      "gem install");

  @Override
  public void initialize(@Nonnull InitContext init) {
    init.register(Body.class, NoBuildToolsInRuntimeCheck::check);
  }

  private static void check(CheckContext ctx, Body body) {
    List<DockerImage> images = body.dockerImages();
    if (images.isEmpty()) {
      return;
    }
    DockerImage runtime = images.get(images.size() - 1);
    runtime.instructions().stream()
        .filter(RunInstruction.class::isInstance)
        .map(RunInstruction.class::cast)
        .forEach(run -> {
          String command = DockerCheckUtils.joinCommandText(run);
          if (installsBuildPackage(command)) {
            ctx.reportIssue(run, MESSAGE);
          }
        });
  }

  static boolean installsBuildPackage(String command) {
    if (command == null || command.isBlank()) {
      return false;
    }
    String lower = " " + command.toLowerCase() + " ";
    boolean looksLikeInstall = INSTALL_HINTS.stream().anyMatch(lower::contains);
    if (!looksLikeInstall) {
      return false;
    }
    return BUILD_PACKAGES.stream().anyMatch(pkg -> containsToken(lower, pkg));
  }

  /** Match a package token bounded by non-alphanumeric chars (avoid {@code git} matching {@code github}). */
  private static boolean containsToken(String haystack, String token) {
    int idx = 0;
    while ((idx = haystack.indexOf(token, idx)) != -1) {
      char before = haystack.charAt(idx - 1);
      int afterIdx = idx + token.length();
      char after = afterIdx < haystack.length() ? haystack.charAt(afterIdx) : ' ';
      boolean leftOk = !Character.isLetterOrDigit(before) && before != '-';
      boolean rightOk = !Character.isLetterOrDigit(after) && after != '-';
      if (leftOk && rightOk) {
        return true;
      }
      idx = afterIdx;
    }
    return false;
  }
}
