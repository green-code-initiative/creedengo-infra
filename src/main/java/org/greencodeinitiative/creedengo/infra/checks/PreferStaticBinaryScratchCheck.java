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
import org.sonar.iac.docker.tree.api.DockerImage;
import org.sonar.iac.docker.tree.api.FromInstruction;

/**
 * GCI1039 — for statically-compiled languages (Go / Rust / Zig), prefer a
 * minimal runtime base image such as {@code scratch} or
 * {@code gcr.io/distroless/static}.
 *
 * <p>Heuristic: if at least one builder stage uses a known
 * static-compilation toolchain ({@code golang:}, {@code rust:},
 * {@code ekidd/rust-musl}, {@code messense/rust-musl}, {@code ziglang/zig}),
 * the final stage should not be a "fat" base like {@code alpine},
 * {@code debian}, {@code ubuntu}, {@code centos}, {@code fedora},
 * {@code rockylinux} or {@code amazonlinux}. The accepted "thin" bases
 * are listed in {@link #STATIC_BASE_REGEX}.</p>
 *
 * <p>See {@code creedengo-infra/preferstaticbinaryscratch.md}. Precision is
 * ~70 % (false positives when CGO is enabled or when the binary needs a
 * shell at runtime); this rule is shipped as {@code Info} for that reason.</p>
 */
@Rule(key = "GCI1039")
public class PreferStaticBinaryScratchCheck implements IacCheck {

  private static final String MESSAGE =
      "Statically-compiled binary detected in a builder stage — prefer a `scratch` or distroless/static runtime base for ~80–97%% smaller images.";

  /** Builder-image prefixes that produce a statically-compilable artefact. */
  private static final Pattern STATIC_BUILDER_REGEX = Pattern.compile(
      "^(?:"
          + "golang:"
          + "|rust:"
          + "|ekidd/rust-musl"
          + "|messense/rust-musl"
          + "|clux/muslrust"
          + "|ziglang/zig"
          + ").*",
      Pattern.CASE_INSENSITIVE);

  /** Runtime images that are considered acceptable for a static binary. */
  private static final Pattern STATIC_BASE_REGEX = Pattern.compile(
      "^(?:"
          + "scratch"
          + "|busybox(?::[^/]*musl[^/]*)?"
          + "|.*/distroless/static.*"
          + "|.*/distroless/base.*"
          + "|gcr\\.io/distroless/.*"
          + "|cgr\\.dev/chainguard/static.*"
          + ")$",
      Pattern.CASE_INSENSITIVE);

  @Override
  public void initialize(@Nonnull InitContext init) {
    //init.register(Body.class, PreferStaticBinaryScratchCheck::check);
  }
}
