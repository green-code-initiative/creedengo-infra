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
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.rule.RuleKey;

/** Unit tests for {@link K8sWorkloadCrossRefSensor} (GCI1043 / GCI1044 / GCI1048). */
class K8sWorkloadCrossRefSensorTest {

  private static final String DEPLOYMENT_HIGH_REPLICAS =
      """
      apiVersion: apps/v1
      kind: Deployment
      metadata:
        name: web
        namespace: prod
        labels:
          app: web
          environment: production
      spec:
        replicas: 6
        selector:
          matchLabels:
            app: web
        template:
          metadata:
            labels: { app: web }
      """;

  private static final String HPA_FOR_WEB =
      """
      apiVersion: autoscaling/v2
      kind: HorizontalPodAutoscaler
      metadata: { name: web, namespace: prod }
      spec:
        scaleTargetRef: { apiVersion: apps/v1, kind: Deployment, name: web }
        minReplicas: 2
        maxReplicas: 10
      """;

  private static final String PDB_FOR_WEB =
      """
      apiVersion: policy/v1
      kind: PodDisruptionBudget
      metadata: { name: web, namespace: prod }
      spec:
        minAvailable: 50%
        selector:
          matchLabels: { app: web }
      """;

  // ---- GCI1043 --------------------------------------------------------------

  @Test
  void gci1043_shouldReportHighReplicasWithoutHpa(@TempDir Path baseDir) {
    SensorContextTester context = newContext(baseDir, "GCI1043");
    addK8s(context, baseDir, "deployment.yaml", DEPLOYMENT_HIGH_REPLICAS);

    new K8sWorkloadCrossRefSensor().execute(context);

    List<Issue> issues = List.copyOf(context.allIssues());
    assertThat(issues).hasSize(1);
    Issue issue = issues.get(0);
    assertThat(issue.ruleKey().rule()).isEqualTo("GCI1043");
    assertThat(issue.primaryLocation().message())
        .contains("replicas=6").contains("> 3").contains("HorizontalPodAutoscaler");
  }

  @Test
  void gci1043_shouldNotReportWhenHpaTargetsDeployment(@TempDir Path baseDir) {
    SensorContextTester context = newContext(baseDir, "GCI1043");
    addK8s(context, baseDir, "all.yaml", DEPLOYMENT_HIGH_REPLICAS + "---\n" + HPA_FOR_WEB);

    new K8sWorkloadCrossRefSensor().execute(context);

    assertThat(context.allIssues()).isEmpty();
  }

  @Test
  void gci1043_shouldHonourQuorumAnnotationOptOut(@TempDir Path baseDir) {
    String quorumYaml =
        """
        apiVersion: apps/v1
        kind: StatefulSet
        metadata:
          name: etcd
          namespace: prod
          annotations:
            eco-design.creedengo.io/quorum: "true"
        spec:
          replicas: 5
          selector: { matchLabels: { app: etcd } }
        """;
    SensorContextTester context = newContext(baseDir, "GCI1043");
    addK8s(context, baseDir, "etcd.yaml", quorumYaml);

    new K8sWorkloadCrossRefSensor().execute(context);

    assertThat(context.allIssues()).isEmpty();
  }

  @Test
  void gci1043_respectsConfigurableThreshold(@TempDir Path baseDir) {
    String yaml =
        """
        apiVersion: apps/v1
        kind: Deployment
        metadata: { name: web, namespace: prod }
        spec:
          replicas: 4
          selector: { matchLabels: { app: web } }
        """;
    SensorContextTester context = newContext(baseDir, "GCI1043");
    // Raise threshold above 4 → no issue.
    context.setSettings(new MapSettings()
        .setProperty(K8sWorkloadCrossRefSensor.PROP_REPLICAS_THRESHOLD, "10"));

    addK8s(context, baseDir, "deployment.yaml", yaml);

    new K8sWorkloadCrossRefSensor().execute(context);

    assertThat(context.allIssues()).isEmpty();
  }

  // ---- GCI1044 --------------------------------------------------------------

  @Test
  void gci1044_shouldReportProductionDeploymentWithoutHpa(@TempDir Path baseDir) {
    String yaml =
        """
        apiVersion: apps/v1
        kind: Deployment
        metadata:
          name: api
          namespace: prod
          labels: { environment: production }
        spec:
          replicas: 2
          selector: { matchLabels: { app: api } }
        """;
    SensorContextTester context = newContext(baseDir, "GCI1044");
    addK8s(context, baseDir, "api.yaml", yaml);

    new K8sWorkloadCrossRefSensor().execute(context);

    List<Issue> issues = List.copyOf(context.allIssues());
    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).ruleKey().rule()).isEqualTo("GCI1044");
    assertThat(issues.get(0).primaryLocation().message())
        .contains("Production Deployment api");
  }

  @Test
  void gci1044_shouldSkipNonProductionDeployments(@TempDir Path baseDir) {
    String yaml =
        """
        apiVersion: apps/v1
        kind: Deployment
        metadata:
          name: api
          namespace: dev
          labels: { environment: staging }
        spec:
          replicas: 2
          selector: { matchLabels: { app: api } }
        """;
    SensorContextTester context = newContext(baseDir, "GCI1044");
    addK8s(context, baseDir, "api.yaml", yaml);

    new K8sWorkloadCrossRefSensor().execute(context);

    assertThat(context.allIssues()).isEmpty();
  }

  // ---- GCI1048 --------------------------------------------------------------

  @Test
  void gci1048_shouldReportMultiReplicaWorkloadWithoutPdb(@TempDir Path baseDir) {
    SensorContextTester context = newContext(baseDir, "GCI1048");
    addK8s(context, baseDir, "deployment.yaml", DEPLOYMENT_HIGH_REPLICAS);

    new K8sWorkloadCrossRefSensor().execute(context);

    List<Issue> issues = List.copyOf(context.allIssues());
    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).ruleKey().rule()).isEqualTo("GCI1048");
    assertThat(issues.get(0).primaryLocation().message())
        .contains("PodDisruptionBudget");
  }

  @Test
  void gci1048_shouldNotReportWhenPdbSelectorMatches(@TempDir Path baseDir) {
    SensorContextTester context = newContext(baseDir, "GCI1048");
    addK8s(context, baseDir, "all.yaml",
        DEPLOYMENT_HIGH_REPLICAS + "---\n" + PDB_FOR_WEB);

    new K8sWorkloadCrossRefSensor().execute(context);

    assertThat(context.allIssues()).isEmpty();
  }

  @Test
  void gci1048_shouldNotReportSingleReplica(@TempDir Path baseDir) {
    String yaml =
        """
        apiVersion: apps/v1
        kind: Deployment
        metadata: { name: cron, namespace: prod }
        spec:
          replicas: 1
          selector: { matchLabels: { app: cron } }
        """;
    SensorContextTester context = newContext(baseDir, "GCI1048");
    addK8s(context, baseDir, "cron.yaml", yaml);

    new K8sWorkloadCrossRefSensor().execute(context);

    assertThat(context.allIssues()).isEmpty();
  }

  // ---- Misc -----------------------------------------------------------------

  @Test
  void shouldNotRunWhenNoRuleIsActive(@TempDir Path baseDir) {
    SensorContextTester context = SensorContextTester.create(baseDir);
    context.setActiveRules(new ActiveRulesBuilder().build());
    addK8s(context, baseDir, "deployment.yaml", DEPLOYMENT_HIGH_REPLICAS);

    new K8sWorkloadCrossRefSensor().execute(context);

    assertThat(context.allIssues()).isEmpty();
  }

  @Test
  void shouldSilentlySkipHelmTemplatesAndKeepOtherFilesWorking(@TempDir Path baseDir) {
    String helmGarbage =
        """
        apiVersion: apps/v1
        kind: Deployment
        metadata: { name: {{ .Release.Name }} }
        spec:
          replicas: {{ .Values.replicaCount }}
        """;
    SensorContextTester context = newContext(baseDir, "GCI1043", "GCI1048");
    addK8s(context, baseDir, "helm-template.yaml", helmGarbage);
    addK8s(context, baseDir, "good.yaml", DEPLOYMENT_HIGH_REPLICAS);

    new K8sWorkloadCrossRefSensor().execute(context);

    // The Helm file is skipped (parse error); the valid one still triggers GCI1043 + GCI1048.
    List<String> ruleKeys = context.allIssues().stream()
        .map(i -> i.ruleKey().rule()).sorted().collect(Collectors.toList());
    assertThat(ruleKeys).containsExactly("GCI1043", "GCI1048");
  }

  @Test
  void labelSelector_parsesKeyValueAndDefaults() {
    K8sWorkloadCrossRefSensor.LabelSelector ls = K8sWorkloadCrossRefSensor.LabelSelector.parse("tier=critical");
    assertThat(ls.key).isEqualTo("tier");
    assertThat(ls.value).isEqualTo("critical");
    assertThat(ls.matches(java.util.Map.of("tier", "critical"))).isTrue();
    assertThat(ls.matches(java.util.Map.of("tier", "low"))).isFalse();

    K8sWorkloadCrossRefSensor.LabelSelector defaults = K8sWorkloadCrossRefSensor.LabelSelector.parse("");
    assertThat(defaults.key).isEqualTo("environment");
    assertThat(defaults.value).isEqualTo("production");
  }

  // ---- Helpers --------------------------------------------------------------

  private static SensorContextTester newContext(Path baseDir, String... activeRules) {
    SensorContextTester context = SensorContextTester.create(baseDir);
    ActiveRulesBuilder builder = new ActiveRulesBuilder();
    for (String key : activeRules) {
      builder.addRule(new NewActiveRule.Builder()
          .setRuleKey(RuleKey.of(InfraKubernetesRulesDefinition.REPOSITORY_KEY, key))
          .setName(key)
          .build());
    }
    context.setActiveRules(builder.build());
    return context;
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
  }
}

