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
 * Lists the terraform check classes contributed by creedengo-infra. The
 * registry is consumed by {@link InfraTerraformRulesDefinition} to load rule
 * metadata via {@code RuleMetadataLoader}, mirroring the {@code creedengo-java}
 * <code>JavaCheckRegistrar</code> pattern.
 */
public final class InfraTerraformCheckRegistrar {

  public static final List<Class<?>> ANNOTATED_RULE_CLASSES = List.of(
      /**org.greencodeinitiative.creedengo.infra.checks.TfPreferGravitonArmCheck.class,
      org.greencodeinitiative.creedengo.infra.checks.TfRightSizeInstanceTypesCheck.class,
      org.greencodeinitiative.creedengo.infra.checks.TfEnableAutoscalingGroupCheck.class,
      org.greencodeinitiative.creedengo.infra.checks.TfStorageLifecycleRulesCheck.class,
      org.greencodeinitiative.creedengo.infra.checks.TfChooseLowCarbonRegionCheck.class,
      org.greencodeinitiative.creedengo.infra.checks.TfAvoidAlwaysOnResourcesCheck.class,
      org.greencodeinitiative.creedengo.infra.checks.TfPreferSpotOrSavingsCheck.class**/
  );

  private InfraTerraformCheckRegistrar() {
    // utility
  }

  public static List<Class<?>> checkClasses() {
    return ANNOTATED_RULE_CLASSES;
  }
}
