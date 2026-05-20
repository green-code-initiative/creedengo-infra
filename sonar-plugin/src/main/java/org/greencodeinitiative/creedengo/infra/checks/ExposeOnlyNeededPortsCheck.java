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
import org.sonar.check.RuleProperty;
import org.sonar.iac.common.api.checks.IacCheck;
import org.sonar.iac.common.api.checks.InitContext;
import org.sonar.iac.docker.tree.api.ExposeInstruction;

/**
 * GCI1034 — Expose only the ports actually used at runtime.
 *
 * <p>{@code EXPOSE} is informational, but downstream orchestrators (Compose,
 * Kubernetes manifests, service meshes) use it to declare service ports and
 * configure load balancers. Excessive {@code EXPOSE} declarations propagate
 * into ingress rule sets and sidecar instrumentation, wasting CPU and memory.</p>
 *
 * <p>An {@code EXPOSE} that lists more than {@link #maxPorts} entries (default
 * {@value DEFAULT_MAX_PORTS}) is flagged.</p>
 */
@Rule(key = "GCI1034")
public class ExposeOnlyNeededPortsCheck implements IacCheck {

  private static final int DEFAULT_MAX_PORTS = 3;
  private static final String MESSAGE_FORMAT =
      "Declare only the ports actually served in production (%d > %d).";

  @RuleProperty(
      key = "maxPorts",
      defaultValue = "" + DEFAULT_MAX_PORTS,
      description = "Maximum number of ports allowed in a single EXPOSE instruction.")
  public int maxPorts = DEFAULT_MAX_PORTS;

  @Override
  public void initialize(@Nonnull InitContext init) {
    init.register(ExposeInstruction.class, (ctx, expose) -> {
      int count = expose.arguments().size();
      if (count > maxPorts) {
        ctx.reportIssue(expose, String.format(MESSAGE_FORMAT, count, maxPorts));
      }
    });
  }
}
