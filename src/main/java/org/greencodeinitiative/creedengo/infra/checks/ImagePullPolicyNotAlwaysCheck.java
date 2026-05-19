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
 * GCI1046 — flag {@code imagePullPolicy: Always} when the image reference
 * is pinned (digest or non-{@code :latest} tag). For such immutable
 * references, {@code Always} only adds wasted registry round-trips on
 * every pod restart — see {@code imagepullpolicynotalways.md}.
 */
@Rule(key = "GCI1046")
public class ImagePullPolicyNotAlwaysCheck implements IacCheck {

  private static final String MESSAGE =
      "Use imagePullPolicy: IfNotPresent on pinned/tagged images to remove redundant registry round-trips.";

  @Override
  public void initialize(@javax.annotation.Nonnull InitContext init) {
    init.register(FileTree.class, ImagePullPolicyNotAlwaysCheck::check);
  }

  private static void check(CheckContext ctx, FileTree file) {
    KubernetesCheckUtils.documents(file).forEach(doc ->
        KubernetesCheckUtils.podSpec(doc).ifPresent(spec ->
            KubernetesCheckUtils.containers(spec).forEach(container -> checkContainer(ctx, container))));
  }

  private static void checkContainer(CheckContext ctx, MappingTree container) {
    Optional<String> policy = KubernetesCheckUtils.scalar(container, "imagePullPolicy");
    if (policy.isEmpty() || !"Always".equalsIgnoreCase(policy.get())) {
      return;
    }
    String image = KubernetesCheckUtils.scalar(container, "image").orElse("");
    if (image.isEmpty() || isPinned(image)) {
      ctx.reportIssue(container, MESSAGE);
    }
  }

  /**
   * Pinned references are: a digest ({@code @sha256:...}) OR an explicit
   * tag that is not {@code :latest}. Untagged images implicitly resolve to
   * {@code :latest} and are NOT flagged here — that case is covered by
   * GCI1031 {@code AvoidLatestTagCheck}.
   */
  private static boolean isPinned(String image) {
    if (image.contains("@sha256:")) {
      return true;
    }
    int colon = image.lastIndexOf(':');
    int slash = image.lastIndexOf('/');
    if (colon <= slash) {
      // no tag (the ':' belongs to a registry port like myreg:5000/img)
      return false;
    }
    String tag = image.substring(colon + 1);
    return !tag.isEmpty() && !"latest".equalsIgnoreCase(tag);
  }
}
