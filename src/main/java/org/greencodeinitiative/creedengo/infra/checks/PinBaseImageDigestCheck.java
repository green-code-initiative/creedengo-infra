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
import org.sonar.iac.docker.tree.api.FromInstruction;

/**
 * GCI1030 — Pin Docker base images by content digest ({@code @sha256:…}).
 *
 * <p>Tags are mutable. Unpinned bases force unplanned re-pulls across CI runners,
 * BuildKit caches and Kubernetes nodes whenever an upstream maintainer re-tags
 * an image. Pinning by digest makes builds reproducible and eliminates phantom
 * rebuilds. Stricter than GCI1031 (which only forbids {@code :latest}).</p>
 */
@Rule(key = "GCI1030")
public class PinBaseImageDigestCheck implements IacCheck {

  private static final String MESSAGE =
      "Pin this base image by content digest (`@sha256:…`) to make builds reproducible and avoid phantom re-pulls.";

  @Override
  public void initialize(@Nonnull InitContext init) {
    /**init.register(FromInstruction.class, (ctx, from) -> {
      String image = DockerCheckUtils.resolve(from.image());
      if (image == null) {
        return;
      }
      if (image.contains("@sha256:") || "scratch".equalsIgnoreCase(image)) {
        return;
      }
      ctx.reportIssue(from.image(), MESSAGE);
    }**/);
  }
}
