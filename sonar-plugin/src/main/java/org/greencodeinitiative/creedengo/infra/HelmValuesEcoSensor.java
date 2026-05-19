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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rule.RuleKey;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;

/**
 * GCI1049 — audit eco-design defaults of Helm chart {@code values.yaml} files.
 *
 * <p>Helm chart defaults propagate to every install: a chart whose default
 * {@code values.yaml} declares {@code replicaCount: 3}, no resources,
 * {@code imagePullPolicy: Always} and {@code autoscaling.enabled: false}
 * spreads wasteful patterns across every consumer of the chart. This Sensor
 * raises one Info-level issue per detected anti-pattern:</p>
 *
 * <ul>
 *   <li><strong>replicas &gt; 1 without autoscaling</strong> — chart starts
 *       over-provisioned and stays so.</li>
 *   <li><strong>empty {@code resources}</strong> — operators get no
 *       requests/limits, kube-scheduler cannot place pods efficiently.</li>
 *   <li><strong>{@code image.pullPolicy: Always}</strong> — every restart
 *       triggers a registry pull, wasting bandwidth.</li>
 *   <li><strong>{@code autoscaling.enabled: false}</strong> — autoscaling
 *       should be the default, not the exception.</li>
 * </ul>
 *
 * <p>Detection runs per Helm chart root: a {@code values.yaml} (or
 * {@code values.yml}) sibling of a {@code Chart.yaml}. Quorum-oriented charts
 * (etcd, Kafka, ZooKeeper, …) can opt-out by adding the annotation
 * {@code creedengo.io/eco-design: quorum} to their {@code Chart.yaml}.</p>
 *
 * <p>Issues are reported on the {@code values.yaml} {@link InputFile} at the
 * line of the offending top-level key when it can be located, or line 1 as a
 * fallback.</p>
 */
public class HelmValuesEcoSensor implements Sensor {

  private static final Logger LOG = LoggerFactory.getLogger(HelmValuesEcoSensor.class);

  static final String RULE_KEY = "GCI1049";
  static final String CHART_FILENAME = "Chart.yaml";
  static final String OPT_OUT_ANNOTATION = "creedengo.io/eco-design";
  static final String OPT_OUT_VALUE = "quorum";

  private static final String MSG_REPLICAS =
      "Helm default `replicaCount: %d` is over-provisioned — set `replicaCount: 1` and enable `autoscaling.enabled: true` so operators opt-in to scaling.";
  private static final String MSG_EMPTY_RESOURCES =
      "Helm default `resources` is empty — declare baseline requests/limits so kube-scheduler can place pods efficiently.";
  private static final String MSG_PULL_ALWAYS =
      "Helm default `image.pullPolicy: Always` triggers a registry pull on every restart — prefer `IfNotPresent` (combine with pinned tags or digests).";
  private static final String MSG_AUTOSCALING_OFF =
      "Helm default `autoscaling.enabled: false` makes scale-to-fit opt-in — enable autoscaling by default so consumers benefit from it out of the box.";

  @Override
  public void describe(SensorDescriptor descriptor) {
    // No language restriction: Helm values.yaml may be classified as yaml,
    // kubernetes, or untyped depending on the consumer project setup.
    descriptor
        .name("Creedengo Infra Helm values.yaml sensor")
        .createIssuesForRuleRepository(InfraKubernetesRulesDefinition.REPOSITORY_KEY);
  }

  @Override
  public void execute(SensorContext context) {
    RuleKey ruleKey = RuleKey.of(InfraKubernetesRulesDefinition.REPOSITORY_KEY, RULE_KEY);
    if (context.activeRules().find(ruleKey) == null) {
      return;
    }
    Iterable<InputFile> candidates = context.fileSystem().inputFiles(
        context.fileSystem().predicates().or(
            context.fileSystem().predicates().hasFilename("values.yaml"),
            context.fileSystem().predicates().hasFilename("values.yml")));
    for (InputFile values : candidates) {
      auditChart(context, ruleKey, values);
    }
  }

  private void auditChart(SensorContext context, RuleKey ruleKey, InputFile values) {
    Path valuesPath;
    try {
      valuesPath = Paths.get(values.uri()).toAbsolutePath().normalize();
    } catch (RuntimeException e) {
      LOG.debug("Cannot resolve filesystem path of {}: {}", values, e.getMessage());
      return;
    }
    Path chartFile = valuesPath.getParent() == null
        ? null
        : valuesPath.getParent().resolve(CHART_FILENAME);
    if (chartFile == null || !Files.isRegularFile(chartFile)) {
      // Not a Helm chart root — ignore.
      return;
    }
    if (isQuorumOptOut(chartFile)) {
      return;
    }

    String content;
    try {
      content = values.contents();
    } catch (IOException e) {
      LOG.debug("Cannot read {}: {}", values, e.getMessage());
      return;
    }
    Node root;
    Map<String, Object> doc;
    try (Reader r1 = new StringReader(content); Reader r2 = new StringReader(content)) {
      Yaml yaml = new Yaml();
      root = yaml.compose(r1);
      Object loaded = yaml.load(r2);
      doc = loaded instanceof Map<?, ?> map ? toStringMap(map) : Map.of();
    } catch (IOException | RuntimeException e) {
      LOG.debug("Skipping {} (YAML parse error): {}", values, e.getMessage());
      return;
    }
    if (doc.isEmpty()) {
      return;
    }
    Map<String, NodeTuple> topLevel = topLevelTuples(root);

    int replicaCount = asInt(doc.get("replicaCount"), 0);
    Map<String, Object> autoscaling = asMap(doc.get("autoscaling"));
    Object autoscalingEnabled = autoscaling.get("enabled");
    boolean autoscalingExplicitlyDisabled = Boolean.FALSE.equals(autoscalingEnabled)
        || "false".equalsIgnoreCase(String.valueOf(autoscalingEnabled));
    boolean autoscalingEnabledTrue = Boolean.TRUE.equals(autoscalingEnabled)
        || "true".equalsIgnoreCase(String.valueOf(autoscalingEnabled));

    if (replicaCount > 1 && !autoscalingEnabledTrue) {
      report(context, ruleKey, values, lineOf(topLevel, "replicaCount"),
          String.format(Locale.ROOT, MSG_REPLICAS, replicaCount));
    }
    Object resources = doc.get("resources");
    if (isEmptyResources(resources)) {
      report(context, ruleKey, values, lineOf(topLevel, "resources"), MSG_EMPTY_RESOURCES);
    }
    Map<String, Object> image = asMap(doc.get("image"));
    String pullPolicy = asString(image.get("pullPolicy"));
    if ("Always".equalsIgnoreCase(pullPolicy)) {
      report(context, ruleKey, values,
          lineOfChild(topLevel.get("image"), "pullPolicy", lineOf(topLevel, "image")),
          MSG_PULL_ALWAYS);
    }
    if (autoscalingExplicitlyDisabled) {
      report(context, ruleKey, values,
          lineOfChild(topLevel.get("autoscaling"), "enabled", lineOf(topLevel, "autoscaling")),
          MSG_AUTOSCALING_OFF);
    }
  }

  // ---- Helpers --------------------------------------------------------------

  /** Detect {@code annotations.creedengo.io/eco-design: quorum} in a {@code Chart.yaml}. */
  static boolean isQuorumOptOut(Path chartFile) {
    try {
      String content = Files.readString(chartFile, StandardCharsets.UTF_8);
      Object loaded = new Yaml().load(content);
      if (!(loaded instanceof Map<?, ?> map)) {
        return false;
      }
      Object annotations = map.get("annotations");
      if (!(annotations instanceof Map<?, ?> annMap)) {
        return false;
      }
      Object value = annMap.get(OPT_OUT_ANNOTATION);
      return value != null && OPT_OUT_VALUE.equalsIgnoreCase(value.toString().trim());
    } catch (IOException | RuntimeException e) {
      LOG.debug("Cannot inspect {} for opt-out annotation: {}", chartFile, e.getMessage());
      return false;
    }
  }

  static boolean isEmptyResources(Object resources) {
    if (resources == null) {
      return true;
    }
    if (resources instanceof Map<?, ?> map) {
      if (map.isEmpty()) {
        return true;
      }
      Object requests = map.get("requests");
      Object limits = map.get("limits");
      boolean reqEmpty = !(requests instanceof Map<?, ?> r) || r.isEmpty();
      boolean limEmpty = !(limits instanceof Map<?, ?> l) || l.isEmpty();
      return reqEmpty && limEmpty;
    }
    return false;
  }

  /** Index every top-level mapping tuple (key + value) by its key name. */
  private static Map<String, NodeTuple> topLevelTuples(Node root) {
    Map<String, NodeTuple> result = new LinkedHashMap<>();
    if (root instanceof MappingNode mapping) {
      for (NodeTuple tuple : mapping.getValue()) {
        if (tuple.getKeyNode() instanceof ScalarNode key) {
          result.put(key.getValue(), tuple);
        }
      }
    }
    return result;
  }

  private static int lineOf(Map<String, NodeTuple> tuples, String key) {
    NodeTuple tuple = tuples.get(key);
    if (tuple == null || tuple.getKeyNode().getStartMark() == null) {
      return 1;
    }
    return tuple.getKeyNode().getStartMark().getLine() + 1;
  }

  private static int lineOfChild(NodeTuple parent, String childKey, int fallback) {
    if (parent == null || !(parent.getValueNode() instanceof MappingNode mapping)) {
      return fallback;
    }
    for (NodeTuple tuple : mapping.getValue()) {
      if (tuple.getKeyNode() instanceof ScalarNode k && childKey.equals(k.getValue())
          && k.getStartMark() != null) {
        return k.getStartMark().getLine() + 1;
      }
    }
    return fallback;
  }

  private static String asString(Object o) {
    return o == null ? null : o.toString();
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> asMap(Object o) {
    if (o instanceof Map<?, ?> map) {
      return toStringMap(map);
    }
    return Map.of();
  }

  private static Map<String, Object> toStringMap(Map<?, ?> map) {
    Map<String, Object> result = new LinkedHashMap<>();
    for (Map.Entry<?, ?> entry : map.entrySet()) {
      result.put(String.valueOf(entry.getKey()), entry.getValue());
    }
    return result;
  }

  private static int asInt(Object o, int fallback) {
    if (o instanceof Number n) {
      return n.intValue();
    }
    if (o instanceof String s) {
      try {
        return Integer.parseInt(s.trim());
      } catch (NumberFormatException ignored) {
        return fallback;
      }
    }
    return fallback;
  }

  private static void report(SensorContext context, RuleKey ruleKey, InputFile file, int line, String message) {
    try {
      NewIssue issue = context.newIssue().forRule(ruleKey);
      NewIssueLocation loc = issue.newLocation()
          .on(file)
          .at(file.selectLine(Math.max(1, line)))
          .message(message);
      issue.at(loc).save();
    } catch (IllegalArgumentException e) {
      LOG.debug("Skipping {} issue on {} (line {}): {}", ruleKey, file, line, e.getMessage());
    }
  }
}


