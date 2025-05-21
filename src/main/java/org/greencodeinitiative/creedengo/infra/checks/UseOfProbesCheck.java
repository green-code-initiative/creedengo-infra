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
  private static final String MESSAGE = "Please configure the probes to ensure the application is healthy and ready.";

  @Override
  public void initialize(@javax.annotation.Nonnull InitContext init) {
    init.register(CommandNode.class, UseOfProbesCheck::checkTree);
  }

  private static void checkTree(CheckContext ctx, CommandNode commandNode) {
    var kubernetesContext = (KubernetesCheckContext) ctx;

    boolean ProbeFound = commandNode.arguments().stream()
      .anyMatch(UseOfProbesCheck::containsProbe);

    if (!ProbeFound) {
      kubernetesContext.reportIssueNoLineShift(commandNode.textRange(), MESSAGE);
    }
    
  }

  private static boolean containsProbe(Node node) {
    if (node instanceof FieldNode fieldNode) {
      return fieldNode.identifiers().stream()
        .anyMatch(id -> id.equalsIgnoreCase("livenessProbe") && id.equalsIgnoreCase("readinessProbe"));
    }
    return false;
  }
}
