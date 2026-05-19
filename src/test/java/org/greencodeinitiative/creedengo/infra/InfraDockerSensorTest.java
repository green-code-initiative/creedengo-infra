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

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.rule.internal.NewActiveRule;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.rule.RuleKey;

/**
 * Smoke test for the {@link InfraDockerSensor} → {@link AbstractInfraIacSensor}
 * pipeline. The four sibling sensors share the same machinery; a single
 * end-to-end test is enough to prove the active-rule filtering, file
 * iteration, parsing and issue reporting are wired correctly.
 */
class InfraDockerSensorTest {

  private static final String DOCKERFILE_USING_LATEST =
      """
      FROM redis:latest
      CMD ["redis-server"]
      """;

  @Test
  void shouldRaiseIssueWhenRuleIsActive(@TempDir Path baseDir) {
    SensorContextTester context = SensorContextTester.create(baseDir);
    activateRules(context, "GCI1031");
    InputFile dockerfile = addDockerfile(context, baseDir, "Dockerfile", DOCKERFILE_USING_LATEST);

    new InfraDockerSensor().execute(context);

    assertThat(context.allIssues())
        .as("AvoidLatestTagCheck (GCI1031) should raise on `FROM redis:latest`")
        .hasSize(1);
    Issue issue = context.allIssues().iterator().next();
    assertThat(issue.ruleKey())
        .isEqualTo(RuleKey.of(InfraDockerRulesDefinition.REPOSITORY_KEY, "GCI1031"));
    assertThat(issue.primaryLocation().inputComponent()).isEqualTo(dockerfile);
    assertThat(issue.primaryLocation().textRange().start().line()).isEqualTo(1);
  }

  @Test
  void shouldNotRunChecksWhenNoRuleIsActive(@TempDir Path baseDir) {
    SensorContextTester context = SensorContextTester.create(baseDir);
    // No active rules → the sensor must early-exit without even parsing.
    context.setActiveRules(new ActiveRulesBuilder().build());
    addDockerfile(context, baseDir, "Dockerfile", DOCKERFILE_USING_LATEST);

    new InfraDockerSensor().execute(context);

    assertThat(context.allIssues()).isEmpty();
  }

  @Test
  void shouldIgnoreFilesOfOtherLanguages(@TempDir Path baseDir) {
    SensorContextTester context = SensorContextTester.create(baseDir);
    activateRules(context, "GCI1031");
    // Same content but language=yaml — the docker sensor must skip it.
    InputFile yamlFile = new TestInputFileBuilder("moduleKey", "values.yaml")
        .setModuleBaseDir(baseDir)
        .setType(InputFile.Type.MAIN)
        .setLanguage("yaml")
        .setCharset(StandardCharsets.UTF_8)
        .setContents(DOCKERFILE_USING_LATEST)
        .build();
    context.fileSystem().add(yamlFile);

    new InfraDockerSensor().execute(context);

    assertThat(context.allIssues()).isEmpty();
  }

  // ---- Helpers --------------------------------------------------------------

  private static void activateRules(SensorContextTester context, String... ruleKeys) {
    ActiveRulesBuilder builder = new ActiveRulesBuilder();
    for (String key : ruleKeys) {
      builder.addRule(new NewActiveRule.Builder()
          .setRuleKey(RuleKey.of(InfraDockerRulesDefinition.REPOSITORY_KEY, key))
          .setName(key)
          .build());
    }
    context.setActiveRules(builder.build());
  }

  private static InputFile addDockerfile(SensorContextTester context, Path baseDir,
      String relativePath, String content) {
    InputFile file = new TestInputFileBuilder("moduleKey", relativePath)
        .setModuleBaseDir(baseDir)
        .setType(InputFile.Type.MAIN)
        .setLanguage(InfraDockerRulesDefinition.LANGUAGE)
        .setCharset(StandardCharsets.UTF_8)
        .setContents(content)
        .build();
    context.fileSystem().add(file);
    return file;
  }
}

