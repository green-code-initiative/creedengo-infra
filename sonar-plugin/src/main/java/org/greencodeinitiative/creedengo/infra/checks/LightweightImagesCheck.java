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

import java.util.Set;
import javax.annotation.Nonnull;
import org.sonar.check.Rule;
import org.sonar.iac.common.api.checks.IacCheck;
import org.sonar.iac.common.api.checks.InitContext;
import org.sonar.iac.docker.tree.api.DockerImage;
import org.sonar.iac.docker.tree.api.FromInstruction;

/**
 * GCI1026 — Prefer a lightweight base image on the last stage of a Dockerfile.
 *
 * <p>Heavy general-purpose images ({@code ubuntu}, {@code debian}, {@code centos},
 * {@code amazonlinux}, the full {@code python:X}/{@code node:X} variants…) embed
 * hundreds of packages that the runtime never uses. The fix is to switch to
 * {@code -alpine}, {@code -slim}, {@code distroless}, {@code chiseled},
 * {@code wolfi}, {@code busybox} or {@code scratch}.</p>
 *
 * <p>Detection is restricted to the last {@code DockerImage} in the file
 * ({@link DockerImage#isLastDockerImageInFile()}); intermediate builder stages
 * are intentionally ignored.</p>
 */
@Rule(key = "GCI1026")
public class LightweightImagesCheck implements IacCheck {

  private static final String MESSAGE =
      "Use a slim/alpine/distroless/scratch base image for the runtime stage — heavy general-purpose images ship unused packages and inflate cold-start cost.";

  /** Base-image name prefixes flagged as "heavy" when used as the final stage. */
  private static final Set<String> HEAVY_PREFIXES = Set.of(
      "ubuntu", "debian", "centos", "amazonlinux", "fedora", "rockylinux", "almalinux",
      "oraclelinux", "redhat/ubi", "registry.access.redhat.com/ubi");

  /** Indicators that the image *is* a lightweight variant — short-circuit the heavy check. */
  private static final Set<String> LIGHT_HINTS = Set.of(
      "alpine", "slim", "distroless", "scratch", "chiseled", "wolfi", "busybox", "minideb",
      "-musl", ":musl", "static");

  @Override
  public void initialize(@Nonnull InitContext init) {
    init.register(org.sonar.iac.docker.tree.api.Body.class, (ctx, body) -> {
      java.util.List<DockerImage> images = body.dockerImages();
      if (images.isEmpty()) {
        return;
      }
      DockerImage image = images.get(images.size() - 1);
      FromInstruction from = image.from();
      String resolved = DockerCheckUtils.resolve(from.image());
      if (resolved == null) {
        return;
      }
      String lower = resolved.toLowerCase();
      if (lower.equals("scratch") || LIGHT_HINTS.stream().anyMatch(lower::contains)) {
        return;
      }
      // Strip registry prefix + digest for the family check.
      String reference = lower;
      int at = reference.indexOf('@');
      if (at >= 0) {
        reference = reference.substring(0, at);
      }
      int lastSlash = reference.lastIndexOf('/');
      String afterSlash = lastSlash >= 0 ? reference.substring(lastSlash + 1) : reference;

      boolean heavy = HEAVY_PREFIXES.stream().anyMatch(lower::startsWith)
          || HEAVY_PREFIXES.stream().anyMatch(reference::contains)
          || isHeavyFamilyTag(afterSlash);
      if (heavy) {
        ctx.reportIssue(from.image(), MESSAGE);
      }
    });
  }

  /**
   * Recognise heavy variants of language images that don't carry a {@code -slim}
   * or {@code -alpine} suffix (e.g. {@code python:3.11}, {@code node:20},
   * {@code openjdk:21}, {@code azul/zulu-openjdk:21}).
   */
  private static boolean isHeavyFamilyTag(String afterSlash) {
    int colon = afterSlash.indexOf(':');
    String name = colon >= 0 ? afterSlash.substring(0, colon) : afterSlash;
    String tag = colon >= 0 ? afterSlash.substring(colon + 1) : "";
    Set<String> heavyFamilies = Set.of(
        "python", "node", "openjdk", "ruby", "php", "golang", "rust",
        "zulu-openjdk", "eclipse-temurin");
    if (!heavyFamilies.contains(name)) {
      return false;
    }
    // Heavy when no lightweight hint appears in the tag.
    return LIGHT_HINTS.stream().noneMatch(tag::contains);
  }
}
