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
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
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
 * Cross-file Kubernetes audit covering three eco-design rules whose detection
 * requires correlating multiple manifests:
 *
 * <ul>
 *   <li><strong>GCI1043</strong> — {@code Deployment.spec.replicas} above a
 *       configurable threshold (default 3) without a matching
 *       {@code HorizontalPodAutoscaler}. Workloads opted-out via annotation
 *       {@code eco-design.creedengo.io/quorum=true} are ignored (etcd, Kafka,
 *       ZooKeeper, …).</li>
 *   <li><strong>GCI1044</strong> — {@code Deployment} carrying a production
 *       label (default {@code environment=production}) without an HPA targeting
 *       it.</li>
 *   <li><strong>GCI1048</strong> — {@code Deployment}/{@code StatefulSet} with
 *       {@code replicas >= 2} not covered by a matching
 *       {@code PodDisruptionBudget}.</li>
 * </ul>
 *
 * <p>The three rules share a single project-wide index built from every
 * Kubernetes manifest of the project, parsed with SnakeYAML. Helm templates
 * containing {@code {{ ... }}} directives are not valid YAML and are silently
 * skipped — Wave-4 will route them through a dedicated
 * {@code KubernetesAnalyzer}-backed sensor (see {@code doc/IMPORT-NOTES.md}
 * §7).</p>
 *
 * <p>The matching {@code IacCheck} stubs
 * ({@link org.greencodeinitiative.creedengo.infra.checks.ReplicasGreaterThanNeededCheck},
 * {@link org.greencodeinitiative.creedengo.infra.checks.RequireHpaForDeploymentCheck},
 * {@link org.greencodeinitiative.creedengo.infra.checks.PreferPdbForRollingEcoCheck})
 * remain registered in
 * {@link InfraKubernetesCheckRegistrar} so {@code RuleMetadataLoader} loads
 * the JSON/HTML rule definitions; detection itself happens here.</p>
 */
public class K8sWorkloadCrossRefSensor implements Sensor {

  private static final Logger LOG = LoggerFactory.getLogger(K8sWorkloadCrossRefSensor.class);

  static final String RULE_REPLICAS = "GCI1043";
  static final String RULE_REQUIRE_HPA = "GCI1044";
  static final String RULE_PDB = "GCI1048";

  static final String PROP_REPLICAS_THRESHOLD = "creedengo.infra.k8s.staticReplicasThreshold";
  static final String PROP_PRODUCTION_SELECTOR = "creedengo.infra.k8s.productionLabelSelector";
  static final String QUORUM_ANNOTATION = "eco-design.creedengo.io/quorum";

  static final int DEFAULT_REPLICAS_THRESHOLD = 3;
  static final String DEFAULT_PRODUCTION_SELECTOR = "environment=production";

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
        .name("Creedengo Infra Kubernetes cross-file sensor")
        .onlyOnLanguage(InfraKubernetesRulesDefinition.LANGUAGE)
        .createIssuesForRuleRepository(InfraKubernetesRulesDefinition.REPOSITORY_KEY);
  }

  @Override
  public void execute(SensorContext context) {
    RuleKey replicasRule = RuleKey.of(InfraKubernetesRulesDefinition.REPOSITORY_KEY, RULE_REPLICAS);
    RuleKey hpaRule = RuleKey.of(InfraKubernetesRulesDefinition.REPOSITORY_KEY, RULE_REQUIRE_HPA);
    RuleKey pdbRule = RuleKey.of(InfraKubernetesRulesDefinition.REPOSITORY_KEY, RULE_PDB);
    boolean activeReplicas = context.activeRules().find(replicasRule) != null;
    boolean activeHpa = context.activeRules().find(hpaRule) != null;
    boolean activePdb = context.activeRules().find(pdbRule) != null;
    if (!activeReplicas && !activeHpa && !activePdb) {
      return;
    }

    int threshold = context.config().getInt(PROP_REPLICAS_THRESHOLD).orElse(DEFAULT_REPLICAS_THRESHOLD);
    String selectorRaw = context.config().get(PROP_PRODUCTION_SELECTOR).orElse(DEFAULT_PRODUCTION_SELECTOR);
    LabelSelector productionSelector = LabelSelector.parse(selectorRaw);

    List<Workload> workloads = new ArrayList<>();
    List<HpaTarget> hpas = new ArrayList<>();
    List<PdbSelector> pdbs = new ArrayList<>();
    Iterable<InputFile> files = context.fileSystem().inputFiles(
        context.fileSystem().predicates().hasLanguage(InfraKubernetesRulesDefinition.LANGUAGE));
    for (InputFile file : files) {
      indexFile(file, workloads, hpas, pdbs);
    }

    for (Workload w : workloads) {
      if (activeReplicas && w.replicas > threshold && !w.quorumOptOut && !hasHpa(w, hpas)) {
        save(context, replicasRule, w,
            String.format(Locale.ROOT,
                "%s/%s declares replicas=%d (> %d) but no HorizontalPodAutoscaler targets it — pair it with an HPA or annotate `%s: true` for quorum services.",
                w.kind, w.name, w.replicas, threshold, QUORUM_ANNOTATION));
      }
      if (activeHpa && w.isDeployment() && productionSelector.matches(w.labels) && !hasHpa(w, hpas)) {
        save(context, hpaRule, w,
            String.format(Locale.ROOT,
                "Production Deployment %s (label %s=%s) is missing a HorizontalPodAutoscaler.",
                w.name, productionSelector.key, productionSelector.value));
      }
      if (activePdb && w.replicas >= 2 && !hasPdb(w, pdbs)) {
        save(context, pdbRule, w,
            String.format(Locale.ROOT,
                "%s/%s runs %d replicas with no PodDisruptionBudget — autoscalers stay conservative and waste node capacity during voluntary disruptions.",
                w.kind, w.name, w.replicas));
      }
    }
  }

  // ---- Indexing -------------------------------------------------------------

  private void indexFile(InputFile file, List<Workload> workloads, List<HpaTarget> hpas, List<PdbSelector> pdbs) {
    String content;
    try {
      content = file.contents();
    } catch (IOException e) {
      LOG.debug("Cannot read {}: {}", file, e.getMessage());
      return;
    }
    Yaml yaml = new Yaml();
    Iterable<Object> docs;
    Iterable<Node> nodes;
    try (Reader r1 = new StringReader(content); Reader r2 = new StringReader(content)) {
      docs = yaml.loadAll(r1);
      nodes = yaml.composeAll(r2);
      var docIter = docs.iterator();
      var nodeIter = nodes.iterator();
      while (docIter.hasNext() && nodeIter.hasNext()) {
        Object doc = docIter.next();
        Node node = nodeIter.next();
        if (!(doc instanceof Map<?, ?> map)) {
          continue;
        }
        int line = node.getStartMark() != null ? node.getStartMark().getLine() + 1 : 1;
        @SuppressWarnings("unchecked")
        Map<String, Object> document = (Map<String, Object>) map;
        indexDocument(file, line, document, workloads, hpas, pdbs);
      }
    } catch (IOException | RuntimeException e) {
      LOG.debug("Skipping {} (YAML parse error — likely Helm template): {}", file, e.getMessage());
    }
  }

  private void indexDocument(InputFile file, int line, Map<String, Object> doc,
      List<Workload> workloads, List<HpaTarget> hpas, List<PdbSelector> pdbs) {
    String kind = asString(doc.get("kind"));
    if (kind == null) {
      return;
    }
    Map<String, Object> metadata = asMap(doc.get("metadata"));
    Map<String, Object> spec = asMap(doc.get("spec"));
    String name = asString(metadata.get("name"));
    String namespace = Objects.requireNonNullElse(asString(metadata.get("namespace")), "default");
    Map<String, String> annotations = asStringMap(metadata.get("annotations"));
    Map<String, String> labels = asStringMap(metadata.get("labels"));

    switch (kind) {
      case "Deployment", "StatefulSet" -> {
        if (name == null) return;
        int replicas = asInt(spec.get("replicas"), 1);
        Map<String, Object> selector = asMap(spec.get("selector"));
        Map<String, String> selectorLabels = asStringMap(selector.get("matchLabels"));
        // Pod template labels are the truth for selection; fall back to metadata.labels.
        Map<String, Object> template = asMap(spec.get("template"));
        Map<String, Object> templateMeta = asMap(template.get("metadata"));
        Map<String, String> templateLabels = asStringMap(templateMeta.get("labels"));
        Map<String, String> effectiveSelectableLabels = new LinkedHashMap<>();
        effectiveSelectableLabels.putAll(labels);
        effectiveSelectableLabels.putAll(templateLabels);
        boolean quorumOptOut = "true".equalsIgnoreCase(annotations.get(QUORUM_ANNOTATION));
        workloads.add(new Workload(file, line, kind, namespace, name, labels,
            selectorLabels.isEmpty() ? effectiveSelectableLabels : selectorLabels,
            replicas, quorumOptOut));
      }
      case "HorizontalPodAutoscaler" -> {
        Map<String, Object> ref = asMap(spec.get("scaleTargetRef"));
        String refKind = asString(ref.get("kind"));
        String refName = asString(ref.get("name"));
        if (refKind != null && refName != null) {
          hpas.add(new HpaTarget(namespace, refKind, refName));
        }
      }
      case "PodDisruptionBudget" -> {
        Map<String, Object> selector = asMap(spec.get("selector"));
        Map<String, String> matchLabels = asStringMap(selector.get("matchLabels"));
        if (!matchLabels.isEmpty()) {
          pdbs.add(new PdbSelector(namespace, matchLabels));
        }
      }
      default -> { /* ignore other kinds */ }
    }
  }

  // ---- Matching -------------------------------------------------------------

  private static boolean hasHpa(Workload w, List<HpaTarget> hpas) {
    for (HpaTarget t : hpas) {
      if (t.namespace.equals(w.namespace) && t.kind.equals(w.kind) && t.name.equals(w.name)) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasPdb(Workload w, List<PdbSelector> pdbs) {
    for (PdbSelector p : pdbs) {
      if (!p.namespace.equals(w.namespace)) {
        continue;
      }
      if (p.matchLabels.isEmpty()) {
        continue;
      }
      boolean allMatch = true;
      for (Map.Entry<String, String> e : p.matchLabels.entrySet()) {
        if (!Objects.equals(w.selectorLabels.get(e.getKey()), e.getValue())) {
          allMatch = false;
          break;
        }
      }
      if (allMatch) {
        return true;
      }
    }
    return false;
  }

  // ---- Reporting ------------------------------------------------------------

  private static void save(SensorContext context, RuleKey rule, Workload w, String message) {
    try {
      NewIssue issue = context.newIssue().forRule(rule);
      NewIssueLocation loc = issue.newLocation()
          .on(w.file)
          .at(w.file.selectLine(Math.max(1, w.line)))
          .message(message);
      issue.at(loc).save();
    } catch (IllegalArgumentException e) {
      LOG.debug("Skipping {} issue on {}: {}", rule, w.file, e.getMessage());
    }
  }

  // ---- Snake-cast helpers ---------------------------------------------------

  private static String asString(Object o) {
    return o == null ? null : o.toString();
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> asMap(Object o) {
    if (o instanceof Map<?, ?> map) {
      Map<String, Object> result = new LinkedHashMap<>();
      for (Map.Entry<?, ?> entry : map.entrySet()) {
        result.put(String.valueOf(entry.getKey()), entry.getValue());
      }
      return result;
    }
    return Collections.emptyMap();
  }

  private static Map<String, String> asStringMap(Object o) {
    if (!(o instanceof Map<?, ?> map)) {
      return Collections.emptyMap();
    }
    Map<String, String> result = new LinkedHashMap<>();
    for (Map.Entry<?, ?> entry : map.entrySet()) {
      result.put(String.valueOf(entry.getKey()),
          entry.getValue() == null ? "" : entry.getValue().toString());
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

  // Reserved for future per-key precise line resolution.
  @SuppressWarnings("unused")
  private static int lineOfKey(Node node, String key) {
    if (node instanceof MappingNode mapping) {
      for (NodeTuple tuple : mapping.getValue()) {
        if (tuple.getKeyNode() instanceof ScalarNode k && key.equals(k.getValue())) {
          return k.getStartMark().getLine() + 1;
        }
      }
    }
    return 1;
  }

  // ---- Records --------------------------------------------------------------

  static final class Workload {
    final InputFile file;
    final int line;
    final String kind;
    final String namespace;
    final String name;
    final Map<String, String> labels;
    final Map<String, String> selectorLabels;
    final int replicas;
    final boolean quorumOptOut;

    Workload(InputFile file, int line, String kind, String namespace, String name,
        Map<String, String> labels, Map<String, String> selectorLabels,
        int replicas, boolean quorumOptOut) {
      this.file = file;
      this.line = line;
      this.kind = kind;
      this.namespace = namespace;
      this.name = name;
      this.labels = labels;
      this.selectorLabels = selectorLabels;
      this.replicas = replicas;
      this.quorumOptOut = quorumOptOut;
    }

    boolean isDeployment() {
      return "Deployment".equals(kind);
    }
  }

  private record HpaTarget(String namespace, String kind, String name) { }

  private record PdbSelector(String namespace, Map<String, String> matchLabels) { }

  /** Simple {@code key=value} label selector — single-pair form only. */
  static final class LabelSelector {
    final String key;
    final String value;

    private LabelSelector(String key, String value) {
      this.key = key;
      this.value = value;
    }

    static LabelSelector parse(String raw) {
      if (raw == null || raw.isBlank()) {
        return new LabelSelector("environment", "production");
      }
      int eq = raw.indexOf('=');
      if (eq <= 0 || eq == raw.length() - 1) {
        return new LabelSelector(raw.trim(), "");
      }
      return new LabelSelector(raw.substring(0, eq).trim(), raw.substring(eq + 1).trim());
    }

    boolean matches(Map<String, String> labels) {
      return labels != null && value.equals(labels.get(key));
    }
  }
}

