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

import org.sonar.check.Rule;
import org.sonar.iac.common.api.checks.CheckContext;
import org.sonar.iac.common.api.checks.IacCheck;
import org.sonar.iac.common.api.checks.InitContext;
import org.sonar.iac.helm.tree.api.CommandNode;
import org.sonar.iac.helm.tree.api.FieldNode;
import org.sonar.iac.helm.tree.api.Node;
import org.sonar.iac.kubernetes.visitors.KubernetesCheckContext;

@Rule(key = "GCI1024")
public class UseOfProbesCheck implements IacCheck {

  private static final String LIVENESS = "livenessProbe";
  private static final String READINESS = "readinessProbe";
  private static final String MESSAGE = "Configure both livenessProbe and readinessProbe to avoid wasted compute on unhealthy or not-yet-ready pods.";

  @Override
  public void initialize(@javax.annotation.Nonnull InitContext init) {
    init.register(CommandNode.class, UseOfProbesCheck::checkTree);
  }

  private static void checkTree(CheckContext ctx, CommandNode commandNode) {
    var kubernetesContext = (KubernetesCheckContext) ctx;

    boolean hasLiveness = commandNode.arguments().stream().anyMatch(n -> isFieldNamed(n, LIVENESS));
    boolean hasReadiness = commandNode.arguments().stream().anyMatch(n -> isFieldNamed(n, READINESS));

    if (!hasLiveness || !hasReadiness) {
      kubernetesContext.reportIssueNoLineShift(commandNode.textRange(), MESSAGE);
    }
  }

  private static boolean isFieldNamed(Node node, String expectedIdentifier) {
    if (node instanceof FieldNode fieldNode) {
      return fieldNode.identifiers().stream()
        .anyMatch(id -> id.equalsIgnoreCase(expectedIdentifier));
    }
    return false;
  }
}
