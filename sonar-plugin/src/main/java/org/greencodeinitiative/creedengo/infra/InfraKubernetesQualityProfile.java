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

import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonarsource.analyzer.commons.BuiltInQualityProfileJsonLoader;

/**
 * Built-in "Creedengo" quality profile for the kubernetes language. The list
 * of activated rule keys is loaded from
 * {@code /org/greencodeinitiative/creedengo/profiles/creedengo_way_profile_kubernetes.json}.
 */
public final class InfraKubernetesQualityProfile implements BuiltInQualityProfilesDefinition {

  private static final String PATH_TO_PROFILE = "/org/greencodeinitiative/creedengo/profiles/creedengo_way_profile_kubernetes.json";
  private static final String PROFILE_NAME = "creedengo way";

  @Override
  public void define(Context context) {
    NewBuiltInQualityProfile profile = context.createBuiltInQualityProfile(PROFILE_NAME, InfraKubernetesRulesDefinition.LANGUAGE);
    BuiltInQualityProfileJsonLoader.load(profile, InfraKubernetesRulesDefinition.REPOSITORY_KEY, PATH_TO_PROFILE);
    profile.done();
  }
}
