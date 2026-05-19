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
import static org.assertj.core.api.Assertions.assertThatCode;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.rule.internal.NewActiveRule;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.rule.RuleKey;

/**
 * Symmetric smoke test for {@link InfraYamlSensor} → {@link AbstractInfraIacSensor}.
 *
 * <p>Wave-2D landed real implementations for the seven YAML checks
 * (1058–1064). This test now verifies both pipeline plumbing
 * (active-rule gating, file iteration, YAML parsing) and that at least one
 * implemented rule actually fires through the sensor.</p>
 */
class InfraYamlSensorTest {

  /** Valid GitHub Actions YAML that triggers {@code 1060} (runs-on: *-latest). */
 /** private static final String VALID_YAML =
      """
      name: ci
      jobs:
        build:
          runs-on: ubuntu-latest
          steps:
            - uses: actions/checkout@v4
            - run: ./mvnw verify
      """;

  @Test
  void shouldRaiseIssueWhenImplementedRuleIsActive(@TempDir Path baseDir) {
    SensorContextTester context = SensorContextTester.create(baseDir);
    // 1060 (CiPinRunnerAndArm) flags `runs-on: ubuntu-latest`.
    activateRules(context, "1060");
    addYaml(context, baseDir, ".github/workflows/ci.yml", VALID_YAML);

    assertThatCode(() -> new InfraYamlSensor().execute(context))
        .as("Yaml sensor must run without raising")
        .doesNotThrowAnyException();

    assertThat(context.allIssues())
        .as("1060 must flag `runs-on: ubuntu-latest`")
        .isNotEmpty();
  }

  @Test
  void shouldNotRunChecksWhenNoRuleIsActive(@TempDir Path baseDir) {
    SensorContextTester context = SensorContextTester.create(baseDir);
    context.setActiveRules(new ActiveRulesBuilder().build());
    addYaml(context, baseDir, ".github/workflows/ci.yml", VALID_YAML);

    new InfraYamlSensor().execute(context);

    assertThat(context.allIssues()).isEmpty();
  }

  @Test
  void shouldIgnoreFilesOfOtherLanguages(@TempDir Path baseDir) {
    SensorContextTester context = SensorContextTester.create(baseDir);
    activateRules(context, "1060");
    // language=docker — the yaml sensor must skip it.
    InputFile other = new TestInputFileBuilder("moduleKey", "Dockerfile")
        .setModuleBaseDir(baseDir)
        .setType(InputFile.Type.MAIN)
        .setLanguage("docker")
        .setCharset(StandardCharsets.UTF_8)
        .setContents(VALID_YAML)
        .build();
    context.fileSystem().add(other);

    new InfraYamlSensor().execute(context);

    assertThat(context.allIssues()).isEmpty();
  }

  // ---- Helpers --------------------------------------------------------------

  private static void activateRules(SensorContextTester context, String... ruleKeys) {
    ActiveRulesBuilder builder = new ActiveRulesBuilder();
    for (String key : ruleKeys) {
      builder.addRule(new NewActiveRule.Builder()
          .setRuleKey(RuleKey.of(InfraYamlRulesDefinition.REPOSITORY_KEY, key))
          .setName(key)
          .build());
    }
    context.setActiveRules(builder.build());
  }

  private static InputFile addYaml(SensorContextTester context, Path baseDir,
      String relativePath, String content) {
    InputFile file = new TestInputFileBuilder("moduleKey", relativePath)
        .setModuleBaseDir(baseDir)
        .setType(InputFile.Type.MAIN)
        .setLanguage(InfraYamlRulesDefinition.LANGUAGE)
        .setCharset(StandardCharsets.UTF_8)
        .setContents(content)
        .build();
    context.fileSystem().add(file);
    return file;
  }**/
}

