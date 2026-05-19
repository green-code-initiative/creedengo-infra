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
import java.util.stream.Collectors;
import org.sonar.iac.docker.symbols.ArgumentResolution;
import org.sonar.iac.docker.tree.api.Argument;
import org.sonar.iac.docker.tree.api.CommandInstruction;
import org.sonar.iac.docker.tree.api.Flag;
import org.sonar.iac.docker.tree.api.HasArguments;

/**
 * Shared helpers for Wave-2B Docker checks. Centralises the noisy bits of
 * resolving {@link Argument}s into strings so individual checks stay focused
 * on their rule logic.
 */
final class DockerCheckUtils {

  private DockerCheckUtils() {
    // utility class
  }

  /** Resolve a single {@link Argument} to its string value, or {@code null} if unresolved/empty. */
  static String resolve(Argument argument) {
    if (argument == null) {
      return null;
    }
    ArgumentResolution resolution = ArgumentResolution.of(argument);
    if (resolution.isUnresolved() || resolution.isEmpty()) {
      return null;
    }
    String value = resolution.value();
    return (value == null || value.isBlank()) ? null : value;
  }

  /** Resolve every argument of a {@link HasArguments} instruction. Unresolved args are dropped. */
  static List<String> resolveAll(HasArguments node) {
    return node.arguments().stream()
      .map(DockerCheckUtils::resolve)
      .filter(s -> s != null)
      .toList();
  }

  /**
   * Join the resolved arguments of a {@link CommandInstruction} (RUN/CMD/ENTRYPOINT) into a
   * single shell-style string. Useful for substring searches on package-manager invocations.
   */
  static String joinCommandText(CommandInstruction instruction) {
    return resolveAll(instruction).stream().collect(Collectors.joining(" "));
  }

  /** Return whether the {@link Flag}s of an instruction include the requested long-form name. */
  static boolean hasFlag(List<Flag> flags, String name) {
    if (flags == null) {
      return false;
    }
    return flags.stream().anyMatch(f -> name.equalsIgnoreCase(f.name()));
  }
}

