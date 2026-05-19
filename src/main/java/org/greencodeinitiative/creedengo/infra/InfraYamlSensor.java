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
import org.sonar.iac.common.yaml.YamlParser;

/**
 * Scan-time sensor running creedengo-infra YAML checks (CI workflows,
 * IaC transverse) against every plain YAML file of the project. Targets the
 * {@code yaml} language so it does not overlap with the more specific
 * {@code kubernetes} / {@code cloudformation} sensors.
 */
public class InfraYamlSensor extends AbstractInfraIacSensor {

  private static final YamlParser PARSER = new YamlParser();

  public InfraYamlSensor() {
    super("Creedengo Infra YAML sensor",
        InfraYamlRulesDefinition.LANGUAGE,
        InfraYamlRulesDefinition.REPOSITORY_KEY);
  }

  @Override
  protected List<Class<?>> checkClasses() {
    return InfraYamlCheckRegistrar.checkClasses();
  }

  @Override
  protected Tree parse(String content) {
    return PARSER.parse(content, null);
  }
}

