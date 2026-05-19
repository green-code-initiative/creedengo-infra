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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.rule.internal.NewActiveRule;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.rule.RuleKey;

/** Unit tests for {@link HelmValuesEcoSensor} (GCI1049). */
class HelmValuesEcoSensorTest {

  private static final String CHART_YAML =
      """
      apiVersion: v2
      name: demo
      version: 0.1.0
      """;

  private static final String CHART_YAML_QUORUM_OPT_OUT =
      """
      apiVersion: v2
      name: etcd
      version: 0.1.0
      annotations:
        creedengo.io/eco-design: quorum
      """;

  private static final String NONCOMPLIANT_VALUES =
      """
      replicaCount: 3
      image:
        repository: nginx
        tag: "1.27"
        pullPolicy: Always
      resources: {}
      autoscaling:
        enabled: false
      """;

  private static final String COMPLIANT_VALUES =
      """
      replicaCount: 1
      image:
        repository: nginx
        tag: "1.27"
        pullPolicy: IfNotPresent
      resources:
        requests: { cpu: "50m", memory: "64Mi" }
        limits:   { memory: "128Mi" }
      autoscaling:
        enabled: true
        minReplicas: 1
        maxReplicas: 4
      """;

  @Test
  void shouldRaiseAllFourIssuesOnNoncompliantDefaults(@TempDir Path baseDir) throws IOException {
    SensorContextTester context = newContext(baseDir);
    Path chartRoot = writeChart(baseDir, "demo", CHART_YAML);
    InputFile values = addValues(context, baseDir, chartRoot, NONCOMPLIANT_VALUES);

    new HelmValuesEcoSensor().execute(context);

    List<Issue> issues = List.copyOf(context.allIssues());
    assertThat(issues).hasSize(4);
    assertThat(issues).allSatisfy(i ->
        assertThat(i.ruleKey()).isEqualTo(
            RuleKey.of(InfraKubernetesRulesDefinition.REPOSITORY_KEY, "GCI1049")));
    assertThat(issues).allSatisfy(i ->
        assertThat(i.primaryLocation().inputComponent()).isEqualTo(values));

    List<String> messages = issues.stream()
        .map(i -> i.primaryLocation().message()).sorted().collect(Collectors.toList());
    assertThat(messages).anyMatch(m -> m.contains("replicaCount"));
    assertThat(messages).anyMatch(m -> m.contains("resources"));
    assertThat(messages).anyMatch(m -> m.contains("pullPolicy"));
    assertThat(messages).anyMatch(m -> m.contains("autoscaling.enabled"));
  }

  @Test
  void shouldReportZeroIssuesOnCompliantDefaults(@TempDir Path baseDir) throws IOException {
    SensorContextTester context = newContext(baseDir);
    Path chartRoot = writeChart(baseDir, "demo", CHART_YAML);
    addValues(context, baseDir, chartRoot, COMPLIANT_VALUES);

    new HelmValuesEcoSensor().execute(context);

    assertThat(context.allIssues()).isEmpty();
  }

  @Test
  void shouldSkipChartWhenQuorumOptOutIsSet(@TempDir Path baseDir) throws IOException {
    SensorContextTester context = newContext(baseDir);
    Path chartRoot = writeChart(baseDir, "etcd", CHART_YAML_QUORUM_OPT_OUT);
    addValues(context, baseDir, chartRoot, NONCOMPLIANT_VALUES);

    new HelmValuesEcoSensor().execute(context);

    assertThat(context.allIssues()).isEmpty();
  }

  @Test
  void shouldIgnoreValuesYamlWithoutSiblingChartYaml(@TempDir Path baseDir) throws IOException {
    // values.yaml in a non-Helm directory must be silently ignored.
    SensorContextTester context = newContext(baseDir);
    Path dir = Files.createDirectories(baseDir.resolve("config"));
    addValues(context, baseDir, dir, NONCOMPLIANT_VALUES);

    new HelmValuesEcoSensor().execute(context);

    assertThat(context.allIssues()).isEmpty();
  }

  @Test
  void shouldNotRunWhenRuleIsInactive(@TempDir Path baseDir) throws IOException {
    SensorContextTester context = SensorContextTester.create(baseDir);
    context.setActiveRules(new ActiveRulesBuilder().build());
    Path chartRoot = writeChart(baseDir, "demo", CHART_YAML);
    addValues(context, baseDir, chartRoot, NONCOMPLIANT_VALUES);

    new HelmValuesEcoSensor().execute(context);

    assertThat(context.allIssues()).isEmpty();
  }

  @Test
  void shouldNotReportReplicaCountWhenAutoscalingEnabledIsTrue(@TempDir Path baseDir) throws IOException {
    // Per the GCI1049 spec, `replicaCount > 1 without autoscaling.enabled` is
    // the smell. With autoscaling on, a higher baseline is acceptable (HPA
    // will scale down). Resources & pullPolicy are compliant, so the chart is
    // fully compliant.
    String values =
        """
        replicaCount: 5
        autoscaling:
          enabled: true
        resources:
          requests: { cpu: "50m" }
        image:
          pullPolicy: IfNotPresent
        """;
    SensorContextTester context = newContext(baseDir);
    Path chartRoot = writeChart(baseDir, "demo", CHART_YAML);
    addValues(context, baseDir, chartRoot, values);

    new HelmValuesEcoSensor().execute(context);

    assertThat(context.allIssues()).isEmpty();
  }

  @Test
  void isEmptyResources_handlesNullEmptyAndPopulated() {
    assertThat(HelmValuesEcoSensor.isEmptyResources(null)).isTrue();
    assertThat(HelmValuesEcoSensor.isEmptyResources(java.util.Map.of())).isTrue();
    assertThat(HelmValuesEcoSensor.isEmptyResources(
        java.util.Map.of("requests", java.util.Map.of()))).isTrue();
    assertThat(HelmValuesEcoSensor.isEmptyResources(
        java.util.Map.of("requests", java.util.Map.of("cpu", "50m")))).isFalse();
  }

  @Test
  void linePositionsPointToTheOffendingKeys(@TempDir Path baseDir) throws IOException {
    // Lines:
    //  1 replicaCount: 3
    //  2 image:
    //  3   pullPolicy: Always
    //  4 resources: {}
    //  5 autoscaling:
    //  6   enabled: false
    String values =
        "replicaCount: 3\n"
            + "image:\n"
            + "  pullPolicy: Always\n"
            + "resources: {}\n"
            + "autoscaling:\n"
            + "  enabled: false\n";
    SensorContextTester context = newContext(baseDir);
    Path chartRoot = writeChart(baseDir, "demo", CHART_YAML);
    addValues(context, baseDir, chartRoot, values);

    new HelmValuesEcoSensor().execute(context);

    java.util.Map<String, Integer> lineByKeyword = new java.util.HashMap<>();
    for (Issue issue : context.allIssues()) {
      String msg = issue.primaryLocation().message();
      int line = issue.primaryLocation().textRange().start().line();
      if (msg.contains("replicaCount")) lineByKeyword.put("replicaCount", line);
      else if (msg.contains("resources")) lineByKeyword.put("resources", line);
      else if (msg.contains("pullPolicy")) lineByKeyword.put("pullPolicy", line);
      else if (msg.contains("autoscaling.enabled")) lineByKeyword.put("autoscaling.enabled", line);
    }
    assertThat(lineByKeyword).containsEntry("replicaCount", 1);
    assertThat(lineByKeyword).containsEntry("pullPolicy", 3);
    assertThat(lineByKeyword).containsEntry("resources", 4);
    assertThat(lineByKeyword).containsEntry("autoscaling.enabled", 6);
  }

  // ---- Helpers --------------------------------------------------------------

  private static SensorContextTester newContext(Path baseDir) {
    SensorContextTester context = SensorContextTester.create(baseDir);
    ActiveRulesBuilder rules = new ActiveRulesBuilder();
    rules.addRule(new NewActiveRule.Builder()
        .setRuleKey(RuleKey.of(InfraKubernetesRulesDefinition.REPOSITORY_KEY, "GCI1049"))
        .setName("GCI1049")
        .build());
    context.setActiveRules(rules.build());
    return context;
  }

  private static Path writeChart(Path baseDir, String chartName, String chartYaml) throws IOException {
    Path chartRoot = Files.createDirectories(baseDir.resolve(chartName));
    Files.writeString(chartRoot.resolve("Chart.yaml"), chartYaml, StandardCharsets.UTF_8);
    return chartRoot;
  }

  private static InputFile addValues(SensorContextTester context, Path baseDir,
      Path chartRoot, String content) throws IOException {
    Path valuesPath = chartRoot.resolve("values.yaml");
    Files.writeString(valuesPath, content, StandardCharsets.UTF_8);
    String relative = baseDir.relativize(valuesPath).toString().replace('\\', '/');
    InputFile file = new TestInputFileBuilder("moduleKey", relative)
        .setModuleBaseDir(baseDir)
        .setType(InputFile.Type.MAIN)
        .setLanguage("yaml")
        .setCharset(StandardCharsets.UTF_8)
        .setContents(content)
        .build();
    context.fileSystem().add(file);
    return file;
  }
}

