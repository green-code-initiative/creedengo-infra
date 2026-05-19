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

import java.util.List;

/**
 * Lists the yaml / CI check classes contributed by creedengo-infra. The
 * registry is consumed by {@link InfraYamlRulesDefinition} to load rule
 * metadata via {@code RuleMetadataLoader}.
 *
 * <p>The list MUST stay in sync with
 * {@code src/main/resources/org/greencodeinitiative/creedengo/profiles/creedengo_way_profile_yaml.json}.
 * If a rule key is referenced by the profile but its class is missing here,
 * SonarQube fails to boot with
 * {@code IllegalStateException: Rule with key 'creedengo-infra-yaml:GCIxxxx' not found}.</p>
 */
public final class InfraYamlCheckRegistrar {

  public static final List<Class<?>> ANNOTATED_RULE_CLASSES = List.of(
      org.greencodeinitiative.creedengo.infra.checks.CiCacheDependenciesCheck.class,             // GCI1058
      org.greencodeinitiative.creedengo.infra.checks.CiSkipRedundantJobsCheck.class,             // GCI1059
      org.greencodeinitiative.creedengo.infra.checks.CiPinRunnerAndArmCheck.class,               // GCI1060
      org.greencodeinitiative.creedengo.infra.checks.IacNoSecretInPlaintextEcoCheck.class,       // GCI1061
      org.greencodeinitiative.creedengo.infra.checks.IacTagEnvironmentEcoCheck.class,            // GCI1062
      org.greencodeinitiative.creedengo.infra.checks.IacPrometheusLowRetentionDefaultCheck.class, // GCI1063
      org.greencodeinitiative.creedengo.infra.checks.IacScheduledScaleDownNonProdCheck.class,    // GCI1064
      org.greencodeinitiative.creedengo.infra.checks.CiPinActionsCheck.class                     // GCI1065
  );

  private InfraYamlCheckRegistrar() {
    // utility
  }

  public static List<Class<?>> checkClasses() {
    return ANNOTATED_RULE_CLASSES;
  }
}

