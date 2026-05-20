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

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sonar.iac.common.api.tree.TextTree;
import org.sonar.iac.common.api.tree.Tree;
import org.sonar.iac.common.yaml.tree.FileTree;
import org.sonar.iac.common.yaml.tree.MappingTree;
import org.sonar.iac.common.yaml.tree.ScalarTree;
import org.sonar.iac.common.yaml.tree.SequenceTree;
import org.sonar.iac.common.yaml.tree.TupleTree;
import org.sonar.iac.common.yaml.tree.YamlTree;

/**
 * Lightweight helpers for navigating a Kubernetes YAML manifest. Centralises
 * the noisy bits of `kind: ...` detection, `spec` / `template.spec` resolution
 * and container iteration so each Wave-2B K8s check stays focused on the rule.
 */
final class KubernetesCheckUtils {

  /** Workloads that expose container specs through {@code spec.template.spec.containers}. */
  static final Set<String> POD_TEMPLATE_KINDS = Set.of(
      "Deployment", "StatefulSet", "DaemonSet", "ReplicaSet", "Job", "CronJob");

  /** Workloads that expose container specs directly under {@code spec.containers}. */
  static final Set<String> POD_KINDS = Set.of("Pod");

  private KubernetesCheckUtils() {
    // utility class
  }

  /** Stream every top-level Kubernetes document (MappingTree) in a YAML file. */
  static Stream<MappingTree> documents(FileTree file) {
    return file.documents().stream()
        .filter(MappingTree.class::isInstance)
        .map(MappingTree.class::cast);
  }

  /** Return the value of the {@code kind:} field of a Kubernetes document. */
  static Optional<String> kind(MappingTree document) {
    return scalar(document, "kind");
  }

  /**
   * Return the pod spec mapping of a Kubernetes document, regardless of whether
   * the document is a {@code Pod} ({@code spec}) or a workload using a pod
   * template ({@code spec.template.spec}).
   */
  static Optional<MappingTree> podSpec(MappingTree document) {
    String k = kind(document).orElse("");
    Optional<MappingTree> spec = mapping(document, "spec");
    if (spec.isEmpty()) {
      return Optional.empty();
    }
    if (POD_KINDS.contains(k)) {
      return spec;
    }
    if (POD_TEMPLATE_KINDS.contains(k)) {
      return spec.flatMap(s -> mapping(s, "template")).flatMap(t -> mapping(t, "spec"));
    }
    return Optional.empty();
  }

  /** Stream the containers (and initContainers) declared in a pod spec. */
  static Stream<MappingTree> containers(MappingTree podSpec) {
    return Stream.concat(
        listOfMappings(podSpec, "containers"),
        listOfMappings(podSpec, "initContainers"));
  }

  /** Return the {@link TupleTree} for a given field of a mapping, if present. */
  static Optional<TupleTree> tuple(MappingTree mapping, String fieldName) {
    return mapping.elements().stream()
        .filter(t -> matchesKey(t, fieldName))
        .findFirst();
  }

  /** Resolve {@code mapping[fieldName]} as a nested {@link MappingTree}, if present. */
  static Optional<MappingTree> mapping(MappingTree mapping, String fieldName) {
    return tuple(mapping, fieldName)
        .map(TupleTree::value)
        .filter(MappingTree.class::isInstance)
        .map(MappingTree.class::cast);
  }

  /** Resolve {@code mapping[fieldName]} as a scalar string, if present. */
  static Optional<String> scalar(MappingTree mapping, String fieldName) {
    return tuple(mapping, fieldName)
        .map(TupleTree::value)
        .flatMap(KubernetesCheckUtils::asScalarString);
  }

  /** Resolve {@code mapping[fieldName]} as a boolean (handles {@code true|yes|on}). */
  static Optional<Boolean> bool(MappingTree mapping, String fieldName) {
    return scalar(mapping, fieldName).map(KubernetesCheckUtils::parseBool);
  }

  /** Extract the scalar text value of a tree node (ScalarTree → its raw value). */
  static Optional<String> asScalarString(Tree value) {
    if (value instanceof ScalarTree scalar) {
      return Optional.ofNullable(scalar.value());
    }
    if (value instanceof TextTree text) {
      return Optional.ofNullable(text.value());
    }
    return Optional.empty();
  }

  /** Match a tuple key by its scalar text (e.g. {@code "containers"}). */
  static boolean matchesKey(TupleTree tuple, String expected) {
    return asScalarString(tuple.key())
        .map(expected::equals)
        .orElse(false);
  }

  private static Stream<MappingTree> listOfMappings(MappingTree podSpec, String fieldName) {
    Optional<TupleTree> tup = tuple(podSpec, fieldName);
    if (tup.isEmpty()) {
      return Stream.empty();
    }
    Tree value = tup.get().value();
    if (!(value instanceof SequenceTree seq)) {
      return Stream.empty();
    }
    return collectElements(seq).stream()
        .filter(MappingTree.class::isInstance)
        .map(MappingTree.class::cast);
  }

  private static List<YamlTree> collectElements(SequenceTree seq) {
    return seq.elements().stream().collect(Collectors.toList());
  }

  private static boolean parseBool(String raw) {
    String v = raw.trim().toLowerCase();
    return v.equals("true") || v.equals("yes") || v.equals("on");
  }
}

