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
import java.util.Set;
import javax.annotation.Nonnull;
import org.sonar.check.Rule;
import org.sonar.iac.common.api.checks.CheckContext;
import org.sonar.iac.common.api.checks.IacCheck;
import org.sonar.iac.common.api.checks.InitContext;
import org.sonar.iac.common.yaml.tree.FileTree;
import org.sonar.iac.common.yaml.tree.MappingTree;
import org.sonar.iac.common.yaml.tree.TupleTree;

/**
 * GCI1040 — every container must declare {@code resources.requests.cpu},
 * {@code resources.requests.memory} and {@code resources.limits.memory}.
 * Batch workloads ({@code Job}, {@code CronJob}) must also declare
 * {@code resources.limits.cpu}.
 *
 * <p>Without requests the scheduler treats the container as zero-cost:
 * nodes get over-committed, the pod lands in {@code BestEffort} QoS (first
 * evicted under pressure), and Cluster Autoscaler / Karpenter cannot make
 * sound scale decisions. Without a memory limit a container can exhaust the
 * node and trigger an OOMKill cascade (re-schedule + re-pull = wasted
 * energy). A CPU limit is mandatory for batch workloads to prevent runaway
 * loops monopolising a node.</p>
 *
 * @see <a href="https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/">
 *      Kubernetes — Resource Management for Pods and Containers</a>
 */
@Rule(key = "GCI1040")
public class SetResourceRequestsAndLimitsCheck implements IacCheck {

  private static final Set<String> BATCH_KINDS = Set.of("Job", "CronJob");

  static final String MSG_NO_RESOURCES =
      "Set resources.requests (cpu, memory) and resources.limits (memory) for every container.";
  static final String MSG_NO_REQUESTS =
      "Set resources.requests.cpu and resources.requests.memory to enable deterministic scheduling.";
  static final String MSG_REQUESTS_CPU =
      "Set resources.requests.cpu to help the scheduler and avoid resource contention.";
  static final String MSG_REQUESTS_MEMORY =
      "Set resources.requests.memory to help the scheduler and avoid resource contention.";
  static final String MSG_NO_LIMITS =
      "Set resources.limits.memory (and resources.limits.cpu for batch workloads) to prevent OOMKill cascades.";
  static final String MSG_LIMITS_MEMORY =
      "Set resources.limits.memory to prevent unbounded memory consumption and OOMKill cascades.";
  static final String MSG_LIMITS_CPU_BATCH =
      "Set resources.limits.cpu for batch workloads (Job/CronJob) to prevent runaway loops monopolising the node.";

  @Override
  public void initialize(@Nonnull InitContext init) {
    init.register(FileTree.class, this::check);
  }

  private void check(CheckContext ctx, FileTree file) {
    KubernetesCheckUtils.documents(file).forEach(doc -> {
      boolean isBatch = KubernetesCheckUtils.kind(doc)
          .map(BATCH_KINDS::contains)
          .orElse(false);
      KubernetesCheckUtils.podSpec(doc).ifPresent(spec ->
          KubernetesCheckUtils.containers(spec)
              .forEach(container -> checkContainer(ctx, container, isBatch)));
    });
  }

  private void checkContainer(CheckContext ctx, MappingTree container, boolean isBatch) {
    Optional<MappingTree> resources = KubernetesCheckUtils.mapping(container, "resources");

    if (resources.isEmpty()) {
      // Report on the "name" key (single-line token)
      ctx.reportIssue(
          KubernetesCheckUtils.tuple(container, "name")
              .map(TupleTree::key)
              .orElse(container.elements().get(0).key()),
          MSG_NO_RESOURCES);
      return;
    }

    checkRequests(ctx, resources.get());
    checkLimits(ctx, resources.get(), isBatch);
  }

  private void checkRequests(CheckContext ctx, MappingTree resources) {
    Optional<TupleTree> requestsTuple = KubernetesCheckUtils.tuple(resources, "requests");

    if (requestsTuple.isEmpty()) {
      // Report on first key in resources block (e.g. "limits" key)
      ctx.reportIssue(resources.elements().get(0).key(), MSG_NO_REQUESTS);
      return;
    }
    MappingTree requests = (MappingTree) requestsTuple.get().value();
    if (KubernetesCheckUtils.scalar(requests, "cpu").isEmpty()) {
      ctx.reportIssue(requestsTuple.get().key(), MSG_REQUESTS_CPU);
    }
    if (KubernetesCheckUtils.scalar(requests, "memory").isEmpty()) {
      ctx.reportIssue(requestsTuple.get().key(), MSG_REQUESTS_MEMORY);
    }
  }

  private void checkLimits(CheckContext ctx, MappingTree resources, boolean isBatch) {
    Optional<TupleTree> limitsTuple = KubernetesCheckUtils.tuple(resources, "limits");

    if (limitsTuple.isEmpty()) {
      // Report on the first key in resources (e.g. "requests" key)
      ctx.reportIssue(resources.elements().get(0).key(), isBatch ? MSG_NO_LIMITS : MSG_LIMITS_MEMORY);
      return;
    }
    MappingTree limits = (MappingTree) limitsTuple.get().value();
    if (KubernetesCheckUtils.scalar(limits, "memory").isEmpty()) {
      ctx.reportIssue(limitsTuple.get().key(), MSG_LIMITS_MEMORY);
    }
    if (isBatch && KubernetesCheckUtils.scalar(limits, "cpu").isEmpty()) {
      ctx.reportIssue(limitsTuple.get().key(), MSG_LIMITS_CPU_BATCH);
    }
  }
}