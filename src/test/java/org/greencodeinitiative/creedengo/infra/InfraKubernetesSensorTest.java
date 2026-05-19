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
import org.sonar.api.rule.RuleKey;

/**
 * Symmetric smoke test for {@link InfraKubernetesSensor} → {@link AbstractInfraIacSensor}.
 * Mirrors {@link InfraDockerSensorTest}: active-rule gating, file iteration,
 * parsing, issue reporting.
 */
class InfraKubernetesSensorTest {

  /**
   * Deployment with one container missing both {@code resources.requests} and
   * {@code resources.limits} entirely. Triggers {@code SetResourceLimitsCheck}
   * (GCI1041) twice on line 9 (cpu + memory).
   */
/**  private static final String DEPLOYMENT_MISSING_LIMITS =
      """
      apiVersion: apps/v1
      kind: Deployment
      metadata:
        name: api
      spec:
        template:
          spec:
            containers:
              - name: api
                image: myorg/api:1
      """;

  @Test
  void shouldRaiseIssueWhenRuleIsActive(@TempDir Path baseDir) {
    SensorContextTester context = SensorContextTester.create(baseDir);
    activateRules(context, "GCI1041");
    InputFile manifest = addK8s(context, baseDir, "deployment.yaml", DEPLOYMENT_MISSING_LIMITS);

    new InfraKubernetesSensor().execute(context);

    assertThat(context.allIssues())
        .as("SetResourceLimitsCheck (GCI1041) should raise on the container missing limits")
        .isNotEmpty();
    assertThat(context.allIssues())
        .allSatisfy(i -> assertThat(i.ruleKey())
            .isEqualTo(RuleKey.of(InfraKubernetesRulesDefinition.REPOSITORY_KEY, "GCI1041")));
    assertThat(context.allIssues())
        .allSatisfy(i -> assertThat(i.primaryLocation().inputComponent()).isEqualTo(manifest));
  }

  @Test
  void shouldNotRunChecksWhenNoRuleIsActive(@TempDir Path baseDir) {
    SensorContextTester context = SensorContextTester.create(baseDir);
    context.setActiveRules(new ActiveRulesBuilder().build());
    addK8s(context, baseDir, "deployment.yaml", DEPLOYMENT_MISSING_LIMITS);

    new InfraKubernetesSensor().execute(context);

    assertThat(context.allIssues()).isEmpty();
  }

  @Test
  void shouldIgnoreFilesOfOtherLanguages(@TempDir Path baseDir) {
    SensorContextTester context = SensorContextTester.create(baseDir);
    activateRules(context, "GCI1041");
    // language=docker — the kubernetes sensor must skip it.
    InputFile other = new TestInputFileBuilder("moduleKey", "Dockerfile")
        .setModuleBaseDir(baseDir)
        .setType(InputFile.Type.MAIN)
        .setLanguage("docker")
        .setCharset(StandardCharsets.UTF_8)
        .setContents(DEPLOYMENT_MISSING_LIMITS)
        .build();
    context.fileSystem().add(other);

    new InfraKubernetesSensor().execute(context);

    assertThat(context.allIssues()).isEmpty();
  }

  // ---- Helpers --------------------------------------------------------------

  private static void activateRules(SensorContextTester context, String... ruleKeys) {
    ActiveRulesBuilder builder = new ActiveRulesBuilder();
    for (String key : ruleKeys) {
      builder.addRule(new NewActiveRule.Builder()
          .setRuleKey(RuleKey.of(InfraKubernetesRulesDefinition.REPOSITORY_KEY, key))
          .setName(key)
          .build());
    }
    context.setActiveRules(builder.build());
  }

  private static InputFile addK8s(SensorContextTester context, Path baseDir,
      String relativePath, String content) {
    InputFile file = new TestInputFileBuilder("moduleKey", relativePath)
        .setModuleBaseDir(baseDir)
        .setType(InputFile.Type.MAIN)
        .setLanguage(InfraKubernetesRulesDefinition.LANGUAGE)
        .setCharset(StandardCharsets.UTF_8)
        .setContents(content)
        .build();
    context.fileSystem().add(file);
    return file;
  }**/
}

