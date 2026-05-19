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
package org.sonar.iac.commons.testing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.util.stream.Stream;

import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.iac.common.reports.AbstractExternalRulesDefinition;
import org.sonar.plugins.java.api.tree.Arguments;

public class AbstractExternalRulesDefinitionAssertions {

  public static void assertExistingRepository(
    RulesDefinition.Context context,
    AbstractExternalRulesDefinition rulesDefinition,
    String reportKey,
    String reportName,
    String language,
    int numberOfRules,
    boolean shouldSupportCCT) {

    assertThat(context.repositories()).hasSize(1);
    var repository = context.repository("external_" + reportKey);
    assertThat(repository).isNotNull();
    assertThat(repository.name()).isEqualTo(reportName);
    assertThat(repository.language()).isEqualTo(language);
    assertThat(repository.isExternal()).isTrue();
    assertThat(repository.rules()).hasSize(numberOfRules);

    for (String ruleKey : rulesDefinition.getRuleLoader().ruleKeys()) {
      assertThatNoException().isThrownBy(() -> rulesDefinition.getRuleLoader().ruleSeverity(ruleKey));
      assertThatNoException().isThrownBy(() -> rulesDefinition.getRuleLoader().ruleType(ruleKey));
    }

    assertThat(rulesDefinition.getRuleLoader().isCleanCodeImpactsAndAttributesSupported()).isEqualTo(shouldSupportCCT);
  }

  public static void assertNoRepositoryIsDefined(RulesDefinition.Context context, AbstractExternalRulesDefinition rulesDefinition) {
    assertThat(context.repositories()).isEmpty();
    assertThat(rulesDefinition.getRuleLoader()).isNull();
  }
}
