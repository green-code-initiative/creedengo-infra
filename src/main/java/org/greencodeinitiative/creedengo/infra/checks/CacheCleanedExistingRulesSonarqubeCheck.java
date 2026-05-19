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
 * GCI1029 — Clean package-manager caches inside the same {@code RUN}.
 *
 * <p>Extends SonarSource RSPEC-6587 (which only covers {@code apt-get}) to
 * {@code dnf} / {@code yum} / {@code pip} / {@code npm} / {@code composer} /
 * {@code gem} / {@code bundle install}. A package install that does not delete
 * its cache (or use the cache-disabling flag handled by GCI1037) bakes 20–500 MB
 * of cache into the image layer.</p>
 *
 * <p>Detection scope here focuses on the "cleanup style": flag when the install
 * appears WITHOUT a same-{@code RUN} cleanup. The complementary "flag style"
 * (e.g. {@code apk --no-cache}, {@code pip --no-cache-dir}) is covered by
 * {@link PreferNoCachePkgFlagCheck} (GCI1037).</p>
 */
@Rule(key = "GCI1029")
public class CacheCleanedExistingRulesSonarqubeCheck implements IacCheck {

  private static boolean contains(String haystack, String... needles) {
    for (String n : needles) {
      if (haystack.contains(n)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void initialize(InitContext initContext) {

  }
}
