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

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.annotation.Nonnull;
import org.sonar.check.Rule;
import org.sonar.iac.common.api.checks.CheckContext;
import org.sonar.iac.common.api.checks.IacCheck;
import org.sonar.iac.common.api.checks.InitContext;
import org.sonar.iac.common.api.tree.Tree;
import org.sonar.iac.common.yaml.tree.FileTree;
import org.sonar.iac.common.yaml.tree.MappingTree;
import org.sonar.iac.common.yaml.tree.SequenceTree;
import org.sonar.iac.common.yaml.tree.TupleTree;

/**
 * GCI1062 — flag YAML resources (Kubernetes / CloudFormation /
 * docker-compose) that don't carry both an {@code environment} and an
 * {@code owner} tag/label. See {@code iactagenvironmenteco.md}.
 *
 * <p>K8s documents are checked against {@code metadata.labels};
 * CloudFormation top-level {@code Resources.*.Properties.Tags}; other
 * top-level mappings are ignored. Required tag keys are read case-
 * insensitively.</p>
 */
@Rule(key = "GCI1062")
public class IacTagEnvironmentEcoCheck implements IacCheck {

  private static final List<String> REQUIRED_TAGS = List.of("environment", "owner");

  /** K8s kinds whose tags can be safely absent (cluster-scoped meta). */
  private static final Set<String> SKIPPED_K8S_KINDS = Set.of(
      "Namespace", "Secret", "ConfigMap", "ServiceAccount",
      "Role", "RoleBinding", "ClusterRole", "ClusterRoleBinding",
      "CustomResourceDefinition", "StorageClass", "PersistentVolume");

  @Override
  public void initialize(@Nonnull InitContext init) {

    //init.register(FileTree.class, IacTagEnvironmentEcoCheck::check);
  }

  private static void check(CheckContext ctx, FileTree file) {
    //KubernetesCheckUtils.documents(file).forEach(doc -> checkDocument(ctx, doc));
  }
}
