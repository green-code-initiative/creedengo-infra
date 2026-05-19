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

import org.junit.jupiter.api.Test;
import org.sonar.iac.common.api.checks.IacCheck;

class CacheCleanedExistingRulesSonarqubeCheckTest {
  private final IacCheck check = new CacheCleanedExistingRulesSonarqubeCheck();

  private static final String MSG_APT =
      "Clean apt caches in the same RUN (`rm -rf /var/lib/apt/lists/*`) to avoid baking them into the image layer.";
  private static final String MSG_DNF =
      "Clean dnf/yum caches in the same RUN (`dnf clean all && rm -rf /var/cache/dnf` or `yum clean all`) to avoid baking them into the image layer.";
  private static final String MSG_PIP =
      "Disable the pip cache in the same RUN (`pip install --no-cache-dir …` or `rm -rf /root/.cache/pip`) to avoid bloating the image layer.";
  private static final String MSG_COMPOSER =
      "Clear the composer cache in the same RUN (`composer clear-cache`) to avoid bloating the image layer.";
  private static final String MSG_GEM =
      "Clean the gem/bundler cache in the same RUN (`rm -rf /usr/local/bundle/cache /root/.bundle`) to avoid bloating the image layer.";

  @Test
  void shouldNotRaiseWhenCacheIsCleaned() {
    DockerVerifier.verifyNoIssue("CacheCleanedExistingRulesSonarqubeCheck/compliant.Dockerfile", check);
  }

  @Test
  void shouldRaiseOnUnclearedCaches() {
    DockerVerifier.verifyIssues("CacheCleanedExistingRulesSonarqubeCheck/noncompliant.Dockerfile", check,
        DockerVerifier.ExpectedIssue.at(2, MSG_APT),
        DockerVerifier.ExpectedIssue.at(3, MSG_DNF),
        DockerVerifier.ExpectedIssue.at(4, MSG_PIP),
        DockerVerifier.ExpectedIssue.at(5, MSG_COMPOSER),
        DockerVerifier.ExpectedIssue.at(6, MSG_GEM));
  }
}

