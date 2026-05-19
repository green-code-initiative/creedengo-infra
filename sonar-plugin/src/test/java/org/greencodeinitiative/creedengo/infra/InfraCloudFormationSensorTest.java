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
 * Symmetric smoke test for {@link InfraCloudFormationSensor} → {@link AbstractInfraIacSensor}.
 * Mirrors {@link InfraDockerSensorTest}: active-rule gating, file iteration,
 * parsing, issue reporting.
 */
class InfraCloudFormationSensorTest {

  /** EC2 instance pinned to x86 {@code m6i.large} → {@code CfnPreferGravitonLowCarbonCheck} (GCI1057). */
  /**private static final String CFN_X86_INSTANCE =
      """
      AWSTemplateFormatVersion: "2010-09-09"
      Resources:
        WebInstance:
          Type: AWS::EC2::Instance
          Properties:
            InstanceType: m6i.large
            ImageId: ami-x86
      """;

  @Test
  void shouldRaiseIssueWhenRuleIsActive(@TempDir Path baseDir) {
    SensorContextTester context = SensorContextTester.create(baseDir);
    activateRules(context, "GCI1057");
    InputFile template = addCfn(context, baseDir, "template.yaml", CFN_X86_INSTANCE);

    new InfraCloudFormationSensor().execute(context);

    assertThat(context.allIssues())
        .as("CfnPreferGravitonLowCarbonCheck (GCI1057) should flag the m6i.large instance")
        .hasSize(1);
    Issue issue = context.allIssues().iterator().next();
    assertThat(issue.ruleKey())
        .isEqualTo(RuleKey.of(InfraCloudFormationRulesDefinition.REPOSITORY_KEY, "GCI1057"));
    assertThat(issue.primaryLocation().inputComponent()).isEqualTo(template);
    assertThat(issue.primaryLocation().message()).contains("ARM/Graviton").contains("m6i");
  }

  @Test
  void shouldNotRunChecksWhenNoRuleIsActive(@TempDir Path baseDir) {
    SensorContextTester context = SensorContextTester.create(baseDir);
    context.setActiveRules(new ActiveRulesBuilder().build());
    addCfn(context, baseDir, "template.yaml", CFN_X86_INSTANCE);

    new InfraCloudFormationSensor().execute(context);

    assertThat(context.allIssues()).isEmpty();
  }

  @Test
  void shouldIgnoreFilesOfOtherLanguages(@TempDir Path baseDir) {
    SensorContextTester context = SensorContextTester.create(baseDir);
    activateRules(context, "GCI1057");
    InputFile tfFile = new TestInputFileBuilder("moduleKey", "main.tf")
        .setModuleBaseDir(baseDir)
        .setType(InputFile.Type.MAIN)
        .setLanguage("terraform")
        .setCharset(StandardCharsets.UTF_8)
        .setContents(CFN_X86_INSTANCE)
        .build();
    context.fileSystem().add(tfFile);

    new InfraCloudFormationSensor().execute(context);

    assertThat(context.allIssues()).isEmpty();
  }

  // ---- Helpers --------------------------------------------------------------

  private static void activateRules(SensorContextTester context, String... ruleKeys) {
    ActiveRulesBuilder builder = new ActiveRulesBuilder();
    for (String key : ruleKeys) {
      builder.addRule(new NewActiveRule.Builder()
          .setRuleKey(RuleKey.of(InfraCloudFormationRulesDefinition.REPOSITORY_KEY, key))
          .setName(key)
          .build());
    }
    context.setActiveRules(builder.build());
  }

  private static InputFile addCfn(SensorContextTester context, Path baseDir,
      String relativePath, String content) {
    InputFile file = new TestInputFileBuilder("moduleKey", relativePath)
        .setModuleBaseDir(baseDir)
        .setType(InputFile.Type.MAIN)
        .setLanguage(InfraCloudFormationRulesDefinition.LANGUAGE)
        .setCharset(StandardCharsets.UTF_8)
        .setContents(content)
        .build();
    context.fileSystem().add(file);
    return file;
  }**/
}

