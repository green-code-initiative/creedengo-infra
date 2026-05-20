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
import java.util.stream.Stream;
import org.sonar.iac.common.yaml.tree.FileTree;
import org.sonar.iac.common.yaml.tree.MappingTree;
import org.sonar.iac.common.yaml.tree.SequenceTree;
import org.sonar.iac.common.yaml.tree.TupleTree;

/**
 * Helpers for navigating GitHub Actions (and look-alike) workflow YAML files.
 *
 * <p>Centralises the noisy bits of {@code jobs.<id>.runs-on} /
 * {@code jobs.<id>.steps[*].run} / {@code on:} navigation so each CI check
 * stays focused on its rule. Reuses the generic primitives exposed by
 * {@link KubernetesCheckUtils} (mapping / scalar / tuple lookup is YAML-
 * model agnostic). Sequences of jobs / steps return as
 * {@link Stream Stream&lt;MappingTree&gt;}.</p>
 */
final class CiYamlCheckUtils {

  private CiYamlCheckUtils() {
    // utility class
  }

  /** First document of the file, if it is a mapping (workflow root). */
  static Optional<MappingTree> root(FileTree file) {
    return KubernetesCheckUtils.documents(file).findFirst();
  }

  /** Stream the jobs of a GitHub Actions workflow ({@code jobs.<id>: { ... }}). */
  static Stream<JobEntry> jobs(MappingTree workflow) {
    return KubernetesCheckUtils.mapping(workflow, "jobs")
        .map(jobs -> jobs.elements().stream()
            .filter(e -> e.value() instanceof MappingTree)
            .map(e -> new JobEntry(
                KubernetesCheckUtils.asScalarString(e.key()).orElse(""),
                (MappingTree) e.value(),
                e)))
        .orElseGet(Stream::empty);
  }

  /** Stream the steps of a job (sequence of mapping nodes). */
  static Stream<MappingTree> steps(MappingTree job) {
    return KubernetesCheckUtils.tuple(job, "steps")
        .map(TupleTree::value)
        .filter(SequenceTree.class::isInstance)
        .map(SequenceTree.class::cast)
        .stream()
        .flatMap(seq -> seq.elements().stream())
        .filter(MappingTree.class::isInstance)
        .map(MappingTree.class::cast);
  }

  /** Look up a scalar under {@code workflow.<key>} (e.g. {@code workflow.concurrency}). */
  static Optional<String> scalar(MappingTree mapping, String key) {
    return KubernetesCheckUtils.scalar(mapping, key);
  }

  /** A single {@code jobs.<id>: { ... }} entry. */
  record JobEntry(String id, MappingTree body, TupleTree tuple) { }
}

