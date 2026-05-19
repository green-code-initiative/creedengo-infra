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
 * GCI1040 — Every container in a Kubernetes workload must declare
 * {@code resources.requests.cpu} and {@code resources.requests.memory}.
 *
 * <p>Without requests the scheduler treats the pod as zero-cost
 * ({@code BestEffort} QoS), breaks bin-packing decisions and blocks
 * autoscaler scale-down — see {@code setresourcerequests.md}.</p>
 */
@Rule(key = "GCI1040")
public class SetResourceRequestsCheck implements IacCheck {

  private static final String MSG_CPU =
      "Set resources.requests.cpu on this container for accurate scheduler bin-packing.";
  private static final String MSG_MEM =
      "Set resources.requests.memory on this container for accurate scheduler bin-packing.";

  @Override
  public void initialize(@javax.annotation.Nonnull InitContext init) {
    //init.register(FileTree.class, SetResourceRequestsCheck::check);
  }
}
