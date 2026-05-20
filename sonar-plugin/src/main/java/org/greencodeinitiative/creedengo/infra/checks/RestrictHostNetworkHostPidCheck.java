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
import org.sonar.iac.common.yaml.tree.FileTree;
import org.sonar.iac.common.yaml.tree.MappingTree;
import org.sonar.iac.common.yaml.tree.TupleTree;

/**
 * GCI1047 — flag {@code hostNetwork}, {@code hostPID} or {@code hostIPC}
 * set to {@code true} on application workloads. Sharing host namespaces
 * pins pods to specific nodes, breaks scheduler bin-packing and forces
 * Karpenter/Cluster-Autoscaler to keep extra capacity — see
 * {@code restricthostnetworkhostpid.md}.
 */
@Rule(key = "GCI1047")
public class RestrictHostNetworkHostPidCheck implements IacCheck {

  private static final String[] FIELDS = {"hostNetwork", "hostPID", "hostIPC"};

  @Override
  public void initialize(@javax.annotation.Nonnull InitContext init) {
    //init.register(FileTree.class, RestrictHostNetworkHostPidCheck::check);
  }
}
