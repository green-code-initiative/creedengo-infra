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
import org.sonar.iac.docker.tree.api.DockerImage;
import org.sonar.iac.docker.tree.api.UserInstruction;

/**
 * GCI1035 — Run containers as a non-root {@code USER}.
 *
 * <p>Root containers magnify the blast radius of every CVE → more emergency
 * redeploys, more fleet-wide re-pulls and unnecessary network/CPU. The last
 * stage of every Dockerfile must declare a non-root {@code USER} (different
 * from {@code root} / {@code 0}).</p>
 */
@Rule(key = "GCI1035")
public class SetNonRootUserCheck implements IacCheck {

  private static final String MESSAGE_MISSING =
      "Declare an explicit non-root USER in the final stage to avoid CVE-driven emergency redeploys.";
  private static final String MESSAGE_ROOT =
      "Avoid USER root / USER 0 in the final stage — switch to a dedicated low-privilege account.";

  @Override
  public void initialize(@Nonnull InitContext init) {
    init.register(org.sonar.iac.docker.tree.api.Body.class, (ctx, body) -> {
      java.util.List<DockerImage> images = body.dockerImages();
      if (images.isEmpty()) {
        return;
      }
      DockerImage image = images.get(images.size() - 1);
      UserInstruction lastUser = image.instructions().stream()
          .filter(UserInstruction.class::isInstance)
          .map(UserInstruction.class::cast)
          .reduce((first, second) -> second) // keep the last USER, like docker semantics
          .orElse(null);

      if (lastUser == null) {
        ctx.reportIssue(image.from(), MESSAGE_MISSING);
        return;
      }
      String value = lastUser.arguments().isEmpty()
          ? null
          : DockerCheckUtils.resolve(lastUser.arguments().get(0));
      if (value == null) {
        return; // unresolved variable → skip rather than false-positive
      }
      String user = value.contains(":") ? value.substring(0, value.indexOf(':')) : value;
      if ("root".equalsIgnoreCase(user.trim()) || "0".equals(user.trim())) {
        ctx.reportIssue(lastUser, MESSAGE_ROOT);
      }
    });
  }
}
