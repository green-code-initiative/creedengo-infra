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
 * Lists the docker check classes contributed by creedengo-infra. The
 * registry is consumed by {@link InfraDockerRulesDefinition} to load rule
 * metadata via {@code RuleMetadataLoader}, mirroring the {@code creedengo-java}
 * <code>JavaCheckRegistrar</code> pattern.
 *
 * <p>The list MUST stay in sync with
 * {@code src/main/resources/org/greencodeinitiative/creedengo/profiles/creedengo_way_profile_docker.json}.
 * If a rule key is referenced by the profile but its class is missing here,
 * SonarQube fails to boot with
 * {@code IllegalStateException: Rule with key 'creedengo-infra-docker:GCIxxxx' not found}.</p>
 */
public final class InfraDockerCheckRegistrar {

  public static final List<Class<?>> ANNOTATED_RULE_CLASSES = List.of(
      org.greencodeinitiative.creedengo.infra.checks.MultistageCheck.class,                       // GCI1025
      org.greencodeinitiative.creedengo.infra.checks.LightweightImagesCheck.class,                // GCI1026
      org.greencodeinitiative.creedengo.infra.checks.InstructionsInSpecificOrderCheck.class,      // GCI1027
      org.greencodeinitiative.creedengo.infra.checks.DeleteUnnecessaryFilesCheck.class,           // GCI1028
      org.greencodeinitiative.creedengo.infra.checks.CacheCleanedExistingRulesSonarqubeCheck.class, // GCI1029
      org.greencodeinitiative.creedengo.infra.checks.PinBaseImageDigestCheck.class,               // GCI1030
      org.greencodeinitiative.creedengo.infra.checks.AvoidLatestTagCheck.class,                   // GCI1031
      org.greencodeinitiative.creedengo.infra.checks.MergeConsecutiveRunCheck.class,              // GCI1032
      org.greencodeinitiative.creedengo.infra.checks.UseCopyNotAddCheck.class,                    // GCI1033
      org.greencodeinitiative.creedengo.infra.checks.ExposeOnlyNeededPortsCheck.class,            // GCI1034
      org.greencodeinitiative.creedengo.infra.checks.SetNonRootUserCheck.class,                   // GCI1035
      org.greencodeinitiative.creedengo.infra.checks.NoBuildToolsInRuntimeCheck.class,            // GCI1036
      org.greencodeinitiative.creedengo.infra.checks.PreferNoCachePkgFlagCheck.class,             // GCI1037
      org.greencodeinitiative.creedengo.infra.checks.SingleProcessPerContainerCheck.class,        // GCI1038
      org.greencodeinitiative.creedengo.infra.checks.PreferStaticBinaryScratchCheck.class         // GCI1039
  );

  private InfraDockerCheckRegistrar() {
    // utility
  }

  public static List<Class<?>> checkClasses() {
    return ANNOTATED_RULE_CLASSES;
  }
}

