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
package org.greencodeinitiative.creedengo.infra;

import java.util.ArrayList;

import org.sonar.api.SonarRuntime;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonarsource.analyzer.commons.RuleMetadataLoader;

/**
 * Declares creedengo-infra rules targeting the kubernetes language in the
 * SonarQube server-side rule repository. Metadata (JSON+HTML) is loaded from
 * the kubernetes classifier jar of {@code creedengo-rules-specifications}.
 */
public final class InfraKubernetesRulesDefinition implements RulesDefinition {

  private static final String RESOURCE_BASE_PATH = "org/green-code-initiative/rules/kubernetes";

  static final String LANGUAGE = "kubernetes";
  static final String REPOSITORY_KEY = "creedengo-infra-kubernetes";
  static final String NAME = "creedengo";

  private final SonarRuntime sonarRuntime;

  public InfraKubernetesRulesDefinition(SonarRuntime sonarRuntime) {
    this.sonarRuntime = sonarRuntime;
  }

  @Override
  public void define(Context context) {
    NewRepository repository = context.createRepository(REPOSITORY_KEY, LANGUAGE).setName(NAME);
    RuleMetadataLoader loader = new RuleMetadataLoader(RESOURCE_BASE_PATH, sonarRuntime);
    loader.addRulesByAnnotatedClass(repository, new ArrayList<>(InfraKubernetesCheckRegistrar.checkClasses()));
    repository.done();
  }

  public String repositoryKey() {
    return REPOSITORY_KEY;
  }
}
