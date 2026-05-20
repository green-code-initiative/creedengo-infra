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

import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.iac.common.api.checks.CheckContext;
import org.sonar.iac.common.api.checks.IacCheck;
import org.sonar.iac.common.api.checks.InitContext;
import org.sonar.iac.common.yaml.tree.FileTree;
import org.sonar.iac.common.yaml.tree.MappingTree;

/**
 * GCI1041 — Every container in a Kubernetes workload must declare
 * {@code resources.limits.cpu} and {@code resources.limits.memory}.
 *
 * <p>Without limits a container can drift into noisy-neighbour territory
 * (OOM-kill cascades, CPU starvation) and break the autoscaler's headroom
 * accounting — see {@code setresourcelimits.md}.</p>
 */
@Rule(key = "GCI1041")
public class SetResourceLimitsCheck implements IacCheck {

  private static final String MSG_CPU =
      "Set resources.limits.cpu on this container to keep noisy-neighbour bursts bounded.";
  private static final String MSG_MEM =
      "Set resources.limits.memory on this container to prevent OOM cascades on the node.";

  @Override
  public void initialize(@javax.annotation.Nonnull InitContext init) {
    //init.register(FileTree.class, SetResourceLimitsCheck::check);
  }
}
