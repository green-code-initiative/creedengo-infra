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
import org.sonar.iac.common.yaml.tree.FileTree;
import org.sonar.iac.common.yaml.tree.MappingTree;

/**
 * GCI1064 — flag {@code Namespace} documents tagged as non-prod that lack
 * an in-file scheduler resource (kube-green {@code SleepInfo}, KEDA cron
 * {@code ScaledObject}, Knative scale-to-zero, etc.). See
 * {@code iacscheduledscaledownnonprod.md}.
 *
 * <p>Heuristic — feasibility ⚠️. File-scoped: a namespace whose scheduler
 * is declared in a sibling YAML file is (knowingly) flagged here. The
 * detection considers two non-prod signals:</p>
 * <ul>
 *   <li>{@code metadata.name} matches a non-prod token
 *       ({@code dev}, {@code test}, {@code staging}, …) or contains
 *       it as a prefix/suffix (e.g. {@code my-staging}).</li>
 *   <li>{@code metadata.labels.environment} (case-insensitive) is in the
 *       non-prod set.</li>
 * </ul>
 *
 * <p>Opt-out: any annotation under {@code metadata.annotations} whose key
 * starts with {@code eco-design/always-on}.</p>
 */
@Rule(key = "GCI1064")
public class IacScheduledScaleDownNonProdCheck implements IacCheck {

  /** Kinds we consider a scheduling resource. */
  private static final Set<String> SCHEDULER_KINDS = Set.of(
      "SleepInfo",          // kube-green
      "ScaledObject",       // KEDA (the cron trigger is a sibling concern, accept any ScaledObject)
      "CronScaledObject",
      "CronJob",            // user-managed kubectl scale CronJob
      "Schedule");          // Crossplane / generic

  /** Non-prod environment tokens. */
  private static final List<String> NONPROD_TOKENS = List.of(
      "dev", "development",
      "test", "testing",
      "staging", "stage",
      "preprod", "preproduction", "pre-prod",
      "qa", "uat", "sandbox");

  @Override
  public void initialize(@Nonnull InitContext init) {
    init.register(FileTree.class, IacScheduledScaleDownNonProdCheck::check);
  }

  private static void check(CheckContext ctx, FileTree file) {
    Set<String> scheduledNamespaces = new HashSet<>();
    boolean hasAnyScheduler = false;
    List<NamespaceDoc> namespaces = new java.util.ArrayList<>();

    for (MappingTree doc : (Iterable<MappingTree>) KubernetesCheckUtils.documents(file)::iterator) {
      String kind = KubernetesCheckUtils.scalar(doc, "kind").orElse("");
      if (SCHEDULER_KINDS.contains(kind)) {
        hasAnyScheduler = true;
        KubernetesCheckUtils.mapping(doc, "metadata")
            .flatMap(m -> KubernetesCheckUtils.scalar(m, "namespace"))
            .ifPresent(scheduledNamespaces::add);
      } else if ("Namespace".equals(kind)) {
        namespaces.add(parseNamespace(doc));
      }
    }

    for (NamespaceDoc ns : namespaces) {
      if (!ns.isNonProd || ns.optOut) {
        continue;
      }
      if (scheduledNamespaces.contains(ns.name) || (hasAnyScheduler && scheduledNamespaces.isEmpty())) {
        // Either a scheduler explicitly references the namespace, or there's a
        // global scheduler without namespace selector — assume covered.
        continue;
      }
      ctx.reportIssue(ns.tree,
          "Non-prod namespace '" + ns.name + "' has no scheduled scale-down resource (kube-green SleepInfo, KEDA cron, Knative scale-to-zero) in this file — add one to power down off-hours.");
    }
  }

  /** Visible for tests. */
  static boolean isNonProdName(String name) {
    if (name == null) {
      return false;
    }
    String n = name.toLowerCase(Locale.ROOT);
    for (String token : NONPROD_TOKENS) {
      if (n.equals(token) || n.startsWith(token + "-") || n.endsWith("-" + token) || n.contains("-" + token + "-")) {
        return true;
      }
    }
    return false;
  }

  private static NamespaceDoc parseNamespace(MappingTree doc) {
    NamespaceDoc result = new NamespaceDoc();
    result.tree = doc;
    MappingTree metadata = KubernetesCheckUtils.mapping(doc, "metadata").orElse(null);
    if (metadata == null) {
      return result;
    }
    result.name = KubernetesCheckUtils.scalar(metadata, "name").orElse("");
    result.isNonProd = isNonProdName(result.name);
    KubernetesCheckUtils.mapping(metadata, "labels").ifPresent(labels -> {
      String env = KubernetesCheckUtils.scalar(labels, "environment").orElse(null);
      if (env != null && NONPROD_TOKENS.contains(env.toLowerCase(Locale.ROOT))) {
        result.isNonProd = true;
      }
    });
    KubernetesCheckUtils.mapping(metadata, "annotations").ifPresent(ann -> {
      for (var t : ann.elements()) {
        String k = KubernetesCheckUtils.asScalarString(t.key()).orElse("");
        if (k.toLowerCase(Locale.ROOT).startsWith("eco-design/always-on")) {
          result.optOut = true;
        }
      }
    });
    return result;
  }

  private static final class NamespaceDoc {
    String name = "";
    boolean isNonProd;
    boolean optOut;
    MappingTree tree;
  }
}
