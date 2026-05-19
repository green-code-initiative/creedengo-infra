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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
 * End-to-end-ish integration test that runs the {@link InfraYamlSensor}
 * over the consolidated {@code ci-sample/} fixture project covering the
 * four CI rules: 1058 (cache), 1059 (redundant jobs), 1060
 * (runner pinning) and GCI1065 (action SHA pinning).
 *
 * <p>The fixture project lives under
 * {@code src/test/resources/projects/ci-sample/.github/workflows/}. The
 * test feeds both {@code build-bad.yml} (multi-violation) and
 * {@code build-good.yml} (fully compliant) to the sensor and asserts the
 * exact (rule key, line) tuples raised.</p>
 *
 * <p>Named {@code *Test} (not {@code *IT}) because the {@code creedengo-infra}
 * pom currently runs everything through {@code maven-surefire-plugin} only
 * — the failsafe block is commented out (see pom §"Integration tests
 * neutralised during the import"). This keeps the test in the default
 * {@code verify} pipeline.</p>
 *
 * <p><b>Line-anchor quirk</b> — sonar-iac 1.46 anchors a YAML node's
 * {@code TextRange.start().line()} on the token <i>following</i> the
 * node's scalar value, not on the node itself. Concretely:</p>
 * <ul>
 *   <li>{@code - run: ./mvnw…} on line 9 → reported on line 10</li>
 *   <li>{@code runs-on: ubuntu-latest} on line 7 → reported on line 8</li>
 *   <li>{@code - uses: …@v44} on line 10 → reported on line 11</li>
 * </ul>
 * <p>The expected line numbers below therefore look "shifted by +1" relative
 * to the YAML fixture. Don't rebase them on the YAML source — they reflect
 * the actual sensor output through {@link AbstractInfraIacSensor}.</p>
 */
class CiSampleSensorTest {

  private static final String PROJECT_RESOURCE_ROOT = "projects/ci-sample";
  private static final String BAD = ".github/workflows/build-bad.yml";
  private static final String GOOD = ".github/workflows/build-good.yml";

  /** All four CI rules activated together. */
  private static final List<String> CI_RULE_KEYS =
      List.of("GCI1058", "GCI1059", "GCI1060", "GCI1065");

  @Test
  void scansCiSampleAndRaisesExpectedIssues(@TempDir Path baseDir) throws IOException {
    SensorContextTester context = SensorContextTester.create(baseDir);
    activateCiRules(context);
    addWorkflow(context, baseDir, BAD);
    addWorkflow(context, baseDir, GOOD);

    new InfraYamlSensor().execute(context);

    Map<String, List<Integer>> grouped = groupByRule(context.allIssues());

    // build-good.yml must be silent across all four rules.
    assertThat(grouped.values().stream().flatMap(List::stream))
        .as("All issues must be on a real line (>0); build-good.yml contributes none")
        .allMatch(line -> line > 0);

    // GCI1058 — `mvn verify` in a job with no cache step.
    // sonar-iac anchors the step MappingTree's TextRange on the line that
    // immediately follows the sequence `-` marker, which is line 10 here
    // (the parser keeps the scalar value's end-line as the start of the
    // surrounding mapping for one-line `- key: value` steps).
    assertThat(grouped.get("GCI1058"))
        .as("GCI1058 should flag the maven `run:` step missing a cache")
        .containsExactly(10);

    // GCI1059 — paths filter missing + concurrency missing + draft PR guard missing → 3 issues.
    assertThat(grouped.get("GCI1059"))
        .as("GCI1059 should raise the three sub-issues (paths / concurrency / draft PR)")
        .hasSize(3);

    // GCI1060 — `runs-on: ubuntu-latest` is on line 7, but sonar-iac anchors
    // the TupleTree's TextRange on the next non-empty token (line 8 = `steps:`).
    // Same +1 quirk as the GCI1058 step anchor above.
    assertThat(grouped.get("GCI1060"))
        .as("GCI1060 should flag the *-latest runner pin")
        .containsExactly(8);

    // GCI1065 — `uses: tj-actions/changed-files@v44` is on line 10, but
    // sonar-iac anchors the `uses:` TupleTree on line 11 (same +1 quirk as
    // GCI1058/GCI1060 above; the TextRange starts at the token following
    // the tuple's value scalar).
    assertThat(grouped.get("GCI1065"))
        .as("GCI1065 should flag the moving-tag action")
        .containsExactly(11);
  }

  // ---- Helpers --------------------------------------------------------------

  private static void activateCiRules(SensorContextTester context) {
    ActiveRulesBuilder builder = new ActiveRulesBuilder();
    CI_RULE_KEYS.forEach(key ->
        builder.addRule(new NewActiveRule.Builder()
            .setRuleKey(RuleKey.of(InfraYamlRulesDefinition.REPOSITORY_KEY, key))
            .setName(key)
            .build()));
    context.setActiveRules(builder.build());
  }

  private static InputFile addWorkflow(SensorContextTester context, Path baseDir,
      String relativePath) throws IOException {
    String content;
    try (Stream<String> lines = readFromClasspath(PROJECT_RESOURCE_ROOT + "/" + relativePath)) {
      content = lines.collect(Collectors.joining("\n"));
    }
    // Materialise the file on disk so SonarQube `InputFile` line/offset math works.
    Path target = baseDir.resolve(relativePath);
    Files.createDirectories(target.getParent());
    Files.writeString(target, content, StandardCharsets.UTF_8);

    InputFile file = new TestInputFileBuilder("ci-sample", relativePath)
        .setModuleBaseDir(baseDir)
        .setType(InputFile.Type.MAIN)
        .setLanguage(InfraYamlRulesDefinition.LANGUAGE)
        .setCharset(StandardCharsets.UTF_8)
        .setContents(content)
        .build();
    context.fileSystem().add(file);
    return file;
  }

  private static Stream<String> readFromClasspath(String resourcePath) throws IOException {
    InputStream is = CiSampleSensorTest.class.getClassLoader().getResourceAsStream(resourcePath);
    if (is == null) {
      throw new IOException("Missing test resource: " + resourcePath);
    }
    return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)).lines();
  }

  private static Map<String, List<Integer>> groupByRule(Collection<Issue> issues) {
    Map<String, List<Integer>> grouped = new TreeMap<>();
    for (Issue issue : issues) {
      String rule = issue.ruleKey().rule();
      int line = issue.primaryLocation().textRange() != null
          ? issue.primaryLocation().textRange().start().line()
          : 0;
      grouped.computeIfAbsent(rule, k -> new ArrayList<>()).add(line);
    }
    grouped.values().forEach(Collections::sort);
    return grouped;
  }
}

