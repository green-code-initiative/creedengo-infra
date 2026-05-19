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
 * Lists the kubernetes check classes contributed by creedengo-infra. The
 * registry is consumed by {@link InfraKubernetesRulesDefinition} to load rule
 * metadata via {@code RuleMetadataLoader}, mirroring the {@code creedengo-java}
 * <code>JavaCheckRegistrar</code> pattern.
 */
public final class InfraKubernetesCheckRegistrar {

  public static final List<Class<?>> ANNOTATED_RULE_CLASSES = List.of(
      org.greencodeinitiative.creedengo.infra.checks.UseOfProbesCheck.class,
      //org.greencodeinitiative.creedengo.infra.checks.SetResourceRequestsCheck.class,
      //org.greencodeinitiative.creedengo.infra.checks.SetResourceLimitsCheck.class,
      org.greencodeinitiative.creedengo.infra.checks.CpuRequestVsLimitRatioCheck.class//,
      /**org.greencodeinitiative.creedengo.infra.checks.ReplicasGreaterThanNeededCheck.class,
      org.greencodeinitiative.creedengo.infra.checks.RequireHpaForDeploymentCheck.class,
      org.greencodeinitiative.creedengo.infra.checks.PreferVpaOrRightsizingCheck.class,
      org.greencodeinitiative.creedengo.infra.checks.ImagePullPolicyNotAlwaysCheck.class,
      org.greencodeinitiative.creedengo.infra.checks.RestrictHostNetworkHostPidCheck.class,
      org.greencodeinitiative.creedengo.infra.checks.PreferPdbForRollingEcoCheck.class,
      org.greencodeinitiative.creedengo.infra.checks.HelmValuesDefaultsEcoCheck.class**/
  );

  private InfraKubernetesCheckRegistrar() {
    // utility
  }

  public static List<Class<?>> checkClasses() {
    return ANNOTATED_RULE_CLASSES;
  }
}
