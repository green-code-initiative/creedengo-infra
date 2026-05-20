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
import org.sonar.iac.common.api.tree.Tree;
import org.sonar.iac.docker.parser.DockerParser;

/**
 * Scan-time sensor running creedengo-infra Docker checks against every
 * Dockerfile of the project. See {@link AbstractInfraIacSensor} for the
 * shared machinery (parser, active-rule filtering, issue reporting).
 */
public class InfraDockerSensor extends AbstractInfraIacSensor {

  private static final DockerParser PARSER = DockerParser.create();

  public InfraDockerSensor() {
    super("Creedengo Infra Docker sensor",
        InfraDockerRulesDefinition.LANGUAGE,
        InfraDockerRulesDefinition.REPOSITORY_KEY);
  }

  @Override
  protected List<Class<?>> checkClasses() {
    return InfraDockerCheckRegistrar.checkClasses();
  }

  @Override
  protected Tree parse(String content) {
    return PARSER.parse(content, null);
  }
}
