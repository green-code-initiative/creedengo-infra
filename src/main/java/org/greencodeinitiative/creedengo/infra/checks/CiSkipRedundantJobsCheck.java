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
import javax.annotation.Nonnull;
import org.sonar.check.Rule;
import org.sonar.iac.common.api.checks.CheckContext;
import org.sonar.iac.common.api.checks.IacCheck;
import org.sonar.iac.common.api.checks.InitContext;
import org.sonar.iac.common.yaml.tree.FileTree;
import org.sonar.iac.common.yaml.tree.MappingTree;
import org.sonar.iac.common.yaml.tree.TupleTree;

/**
 * 1059 — flag GitHub-Actions workflows that miss the three "skip
 * redundant work" patterns: {@code paths}/{@code paths-ignore} filter on
 * the trigger, top-level {@code concurrency} group with
 * {@code cancel-in-progress}, and a draft-PR skip on PR-triggered jobs.
 *
 * <p>Reports one Info-level issue per workflow root for each missing
 * pattern. PR-triggered jobs without a {@code if: ... draft == false}
 * guard are flagged at the job tuple. See
 * {@code ciskipredundantjobs.md}.</p>
 */
@Rule(key = "GCI1059")
public class CiSkipRedundantJobsCheck implements IacCheck {

  static final String MSG_PATHS =
      "Declare a `paths:` / `paths-ignore:` filter so docs-only commits don't trigger this workflow.";
  static final String MSG_CONCURRENCY =
      "Declare a top-level `concurrency:` group with `cancel-in-progress: true` so newer commits cancel in-flight runs.";
  static final String MSG_DRAFT_PR =
      "Skip draft pull requests (e.g. `if: github.event.pull_request.draft == false`).";

  @Override
  public void initialize(@Nonnull InitContext init) {
    init.register(FileTree.class, CiSkipRedundantJobsCheck::check);
  }

  private static void check(CheckContext ctx, FileTree file) {
    Optional<MappingTree> rootOpt = CiYamlCheckUtils.root(file);
    if (rootOpt.isEmpty()) {
      return;
    }
    MappingTree workflow = rootOpt.get();
    TriggerInfo triggers = parseTriggers(workflow);
    if (!triggers.declared) {
      return; // Likely not a GitHub workflow at all (no `on:` key).
    }

    if (!triggers.hasPaths) {
      onTuple(workflow).ifPresentOrElse(
          on -> ctx.reportIssue(on, MSG_PATHS),
          () -> ctx.reportIssue(workflow, MSG_PATHS));
    }
    if (!hasConcurrencyCancel(workflow)) {
      KubernetesCheckUtils.tuple(workflow, "concurrency").ifPresentOrElse(
          c -> ctx.reportIssue(c, MSG_CONCURRENCY),
          () -> ctx.reportIssue(workflow, MSG_CONCURRENCY));
    }
    if (triggers.hasPullRequest) {
      CiYamlCheckUtils.jobs(workflow)
          .filter(job -> !skipsDraftPr(job.body()))
          .forEach(job -> ctx.reportIssue(job.tuple(), MSG_DRAFT_PR));
    }
  }

  /**
   * Look up the workflow trigger tuple. YAML 1.1 normalises the bare key
   * {@code on:} to the boolean {@code true:} — some sonar-iac parser versions
   * surface it that way, so probe both spellings.
   */
  private static Optional<TupleTree> onTuple(MappingTree workflow) {
    Optional<TupleTree> direct = KubernetesCheckUtils.tuple(workflow, "on");
    return direct.isPresent() ? direct : KubernetesCheckUtils.tuple(workflow, "true");
  }

  /** Visible for tests. */
  static TriggerInfo parseTriggers(MappingTree workflow) {
    TriggerInfo info = new TriggerInfo();
    TupleTree onTuple = onTuple(workflow).orElse(null);
    if (onTuple == null) {
      return info;
    }
    info.declared = true;
    Object onValue = onTuple.value();
    if (onValue instanceof MappingTree mapping) {
      // `on: { push: { paths: [...] }, pull_request: ... }`
      mapping.elements().forEach(e -> {
        String key = KubernetesCheckUtils.asScalarString(e.key()).orElse("");
        if ("pull_request".equals(key) || "pull_request_target".equals(key)) {
          info.hasPullRequest = true;
        }
        if (e.value() instanceof MappingTree triggerMap) {
          if (KubernetesCheckUtils.tuple(triggerMap, "paths").isPresent()
              || KubernetesCheckUtils.tuple(triggerMap, "paths-ignore").isPresent()) {
            info.hasPaths = true;
          }
        }
      });
    } else {
      // `on: push` or `on: [push, pull_request]` — no paths filter possible.
      String raw = onTuple.value().toString().toLowerCase();
      info.hasPullRequest = raw.contains("pull_request");
    }
    return info;
  }

  private static boolean hasConcurrencyCancel(MappingTree workflow) {
    return KubernetesCheckUtils.mapping(workflow, "concurrency")
        .flatMap(c -> KubernetesCheckUtils.scalar(c, "cancel-in-progress"))
        .map(v -> "true".equalsIgnoreCase(v.trim()))
        .orElse(false);
  }

  /** Visible for tests. */
  static boolean skipsDraftPr(MappingTree job) {
    String guard = KubernetesCheckUtils.scalar(job, "if").orElse("");
    String normalised = guard.replaceAll("\\s+", "").toLowerCase();
    return normalised.contains("pull_request.draft==false")
        || normalised.contains("!github.event.pull_request.draft");
  }

  static final class TriggerInfo {
    boolean declared;
    boolean hasPaths;
    boolean hasPullRequest;
  }
}
