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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NoBuildToolsInRuntimeCheckTest {
/**
  private static final String MESSAGE =
      "Move build tools to a separate builder stage — keeping compilers/SDKs in the runtime image inflates size and CVE surface.";

  @Test
  void compliant() {
    // Builder may install gcc/make (it's not the last stage), runtime only installs tini.
    DockerVerifier.verifyNoIssue("NoBuildToolsInRuntimeCheck/compliant.Dockerfile",
        new NoBuildToolsInRuntimeCheck());
  }

  @Test
  void noncompliant() {
    // Two RUNs in the runtime (last) stage install build tooling: lines 7 and 9.
    // Builder stage's `apk add gcc make` (line 2) MUST NOT be flagged.
    DockerVerifier.verifyIssues("NoBuildToolsInRuntimeCheck/noncompliant.Dockerfile",
        new NoBuildToolsInRuntimeCheck(),
        at(7, MESSAGE), at(9, MESSAGE));
  }

  @Test
  void installsBuildPackage_matchesCommonInvocations() {
    assertThat(NoBuildToolsInRuntimeCheck.installsBuildPackage("apk add --no-cache gcc")).isTrue();
    assertThat(NoBuildToolsInRuntimeCheck.installsBuildPackage("apt-get install -y build-essential")).isTrue();
    assertThat(NoBuildToolsInRuntimeCheck.installsBuildPackage("pip install poetry")).isTrue();
    assertThat(NoBuildToolsInRuntimeCheck.installsBuildPackage("npm install -g yarn")).isTrue();
  }

  @Test
  void installsBuildPackage_ignoresFalsePositives() {
    // No package manager invocation
    assertThat(NoBuildToolsInRuntimeCheck.installsBuildPackage("echo gcc make")).isFalse();
    // 'git' must NOT match 'github'
    assertThat(NoBuildToolsInRuntimeCheck.installsBuildPackage("apk add github-runner")).isFalse();
    // Empty / null
    assertThat(NoBuildToolsInRuntimeCheck.installsBuildPackage("")).isFalse();
    assertThat(NoBuildToolsInRuntimeCheck.installsBuildPackage(null)).isFalse();
  }**/
}

