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

import javax.annotation.Nonnull;
import org.sonar.check.Rule;
import org.sonar.iac.common.api.checks.IacCheck;
import org.sonar.iac.common.api.checks.InitContext;
import org.sonar.iac.docker.tree.api.RunInstruction;

/**
 * GCI1037 — Use {@code --no-cache} / {@code --no-cache-dir} for package installs.
 *
 * <p>Positive-form complement to {@link CacheCleanedExistingRulesSonarqubeCheck}
 * (GCI1029) for package managers where a no-cache flag is idiomatic:
 * {@code apk}, {@code pip}, {@code npm}.</p>
 */
@Rule(key = "GCI1037")
public class PreferNoCachePkgFlagCheck implements IacCheck {

  private static final String MSG_APK =
      "Use `apk add --no-cache …` so no APK cache is baked into the image layer.";
  private static final String MSG_PIP =
      "Use `pip install --no-cache-dir …` so the pip cache is not baked into the image layer.";
  private static final String MSG_NPM =
      "Use `npm install --no-cache` (or `npm ci --prefer-offline --no-audit --no-cache`) to avoid the npm cache layer.";

  @Override
  public void initialize(@Nonnull InitContext init) {
    /**init.register(RunInstruction.class, (ctx, run) -> {
      String cmd = DockerCheckUtils.joinCommandText(run).toLowerCase();
      if (cmd.isBlank()) {
        return;
      }
      if (cmd.contains("apk add") && !cmd.contains("--no-cache")) {
        ctx.reportIssue(run, MSG_APK);
      }
      if (cmd.contains("pip install") && !cmd.contains("--no-cache-dir")) {
        ctx.reportIssue(run, MSG_PIP);
      }
      if ((cmd.contains("npm install") || cmd.contains("npm ci") || cmd.contains("npm i "))
          && !cmd.contains("--no-cache")) {
        ctx.reportIssue(run, MSG_NPM);
      }
    });**/
  }
}
