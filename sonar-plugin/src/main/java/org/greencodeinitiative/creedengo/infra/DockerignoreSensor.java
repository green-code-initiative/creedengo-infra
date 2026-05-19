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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rule.RuleKey;

/**
 * GCI1028 — cross-file audit of {@code .dockerignore} files.
 *
 * <p>The Docker build context is sent in full to the daemon before any
 * instruction executes. Without a curated {@code .dockerignore}, projects
 * routinely ship {@code node_modules/}, {@code target/}, {@code .git/},
 * {@code .env}, IDE files and build outputs into the build context — bloating
 * builds and risking secret leakage via permissive {@code COPY . .}
 * instructions.</p>
 *
 * <p>This {@link Sensor} runs independently of the sonar-iac AST pipeline
 * because {@code .dockerignore} is not indexed as a Sonar input file. For
 * every Dockerfile of the project it:</p>
 * <ol>
 *   <li>looks up a sibling {@code .dockerignore} in the same directory, then
 *       walks up to the project base directory (monorepo fallback);</li>
 *   <li>if absent, raises a Major issue on the Dockerfile's first line;</li>
 *   <li>if present, infers the runtime stack from the <em>last</em>
 *       {@code FROM} instruction and reports any missing baseline entries from
 *       the matching template (Node, Maven/Gradle, Python, generic).</li>
 * </ol>
 *
 * <p>See {@code creedengo-infra/deleteunnecessaryfiles.md} for the design
 * write-up.</p>
 */
public class DockerignoreSensor implements Sensor {

  private static final Logger LOG = LoggerFactory.getLogger(DockerignoreSensor.class);

  static final String RULE_KEY = "GCI1028";
  static final String DOCKERIGNORE_FILENAME = ".dockerignore";

  private static final String MSG_MISSING_FILE =
      "Add a `.dockerignore` next to this Dockerfile to keep the build context small and avoid shipping VCS metadata, dependency caches or secrets.";
  private static final String MSG_MISSING_ENTRIES =
      "`.dockerignore` is missing recommended entries for a %s build context: %s.";

  /** Per-stack baseline of entries that should always be ignored. */
  private static final Map<Stack, List<String>> TEMPLATES = Map.of(
      Stack.NODE, List.of("node_modules", ".git", ".env"),
      Stack.JVM, List.of("target", ".git", ".env"),
      Stack.PYTHON, List.of("__pycache__", ".git", ".env"),
      Stack.GENERIC, List.of(".git", ".env")
  );

  /** Last {@code FROM <image>} per file. Comments / continuations supported. */
  private static final Pattern FROM_PATTERN =
      Pattern.compile("(?im)^\\s*FROM\\s+(?:--platform=\\S+\\s+)?([^\\s#]+)");

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
        .name("Creedengo Infra .dockerignore sensor")
        .onlyOnLanguage(InfraDockerRulesDefinition.LANGUAGE)
        .createIssuesForRuleRepository(InfraDockerRulesDefinition.REPOSITORY_KEY);
  }

  @Override
  public void execute(SensorContext context) {
    RuleKey ruleKey = RuleKey.of(InfraDockerRulesDefinition.REPOSITORY_KEY, RULE_KEY);
    if (context.activeRules().find(ruleKey) == null) {
      return;
    }
    Path baseDir = context.fileSystem().baseDir().toPath().toAbsolutePath().normalize();
    Iterable<InputFile> dockerfiles = context.fileSystem().inputFiles(
        context.fileSystem().predicates().hasLanguage(InfraDockerRulesDefinition.LANGUAGE));
    for (InputFile dockerfile : dockerfiles) {
      auditDockerfile(context, ruleKey, baseDir, dockerfile);
    }
  }

  private void auditDockerfile(SensorContext context, RuleKey ruleKey, Path baseDir, InputFile dockerfile) {
    Path dockerPath;
    try {
      dockerPath = Paths.get(dockerfile.uri()).toAbsolutePath().normalize();
    } catch (RuntimeException e) {
      LOG.debug("Cannot resolve filesystem path of {}: {}", dockerfile, e.getMessage());
      return;
    }
    Path ignoreFile = findNearest(dockerPath.getParent(), baseDir);
    if (ignoreFile == null) {
      report(context, ruleKey, dockerfile, MSG_MISSING_FILE);
      return;
    }
    Set<String> entries = readEntries(ignoreFile);
    if (entries == null) {
      // Could not read — be lenient.
      return;
    }
    Stack stack = inferStack(dockerfile);
    List<String> missing = new ArrayList<>();
    for (String required : TEMPLATES.get(stack)) {
      if (!entries.contains(required)) {
        missing.add(required);
      }
    }
    if (!missing.isEmpty()) {
      report(context, ruleKey, dockerfile,
          String.format(Locale.ROOT, MSG_MISSING_ENTRIES, stack.label, String.join(", ", missing)));
    }
  }

  // ---- Filesystem helpers ---------------------------------------------------

  /** Walk from {@code start} up to (and including) {@code baseDir} looking for {@code .dockerignore}. */
  static Path findNearest(Path start, Path baseDir) {
    if (start == null) {
      return null;
    }
    Path current = start.toAbsolutePath().normalize();
    Path stop = baseDir == null ? null : baseDir.toAbsolutePath().normalize();
    while (current != null) {
      Path candidate = current.resolve(DOCKERIGNORE_FILENAME);
      if (Files.isRegularFile(candidate)) {
        return candidate;
      }
      if (stop != null && current.equals(stop)) {
        break;
      }
      current = current.getParent();
    }
    return null;
  }

  static Set<String> readEntries(Path ignoreFile) {
    try {
      List<String> lines = Files.readAllLines(ignoreFile, StandardCharsets.UTF_8);
      Set<String> entries = new LinkedHashSet<>();
      for (String raw : lines) {
        String line = raw.trim();
        if (line.isEmpty() || line.startsWith("#")) {
          continue;
        }
        // Strip leading '!' (negation) and trailing slash for matching.
        if (line.startsWith("!")) {
          line = line.substring(1).trim();
        }
        if (line.endsWith("/")) {
          line = line.substring(0, line.length() - 1);
        }
        if (!line.isEmpty()) {
          entries.add(line);
        }
      }
      return entries;
    } catch (IOException e) {
      LOG.debug("Cannot read {}: {}", ignoreFile, e.getMessage());
      return null;
    }
  }

  // ---- Stack inference ------------------------------------------------------

  enum Stack {
    NODE("Node.js"),
    JVM("JVM/Maven"),
    PYTHON("Python"),
    GENERIC("generic");

    final String label;

    Stack(String label) {
      this.label = label;
    }
  }

  static Stack inferStack(InputFile dockerfile) {
    String content;
    try {
      content = dockerfile.contents();
    } catch (IOException e) {
      return Stack.GENERIC;
    }
    return inferStackFromContent(content);
  }

  static Stack inferStackFromContent(String content) {
    Matcher matcher = FROM_PATTERN.matcher(content);
    String lastImage = null;
    while (matcher.find()) {
      lastImage = matcher.group(1).toLowerCase(Locale.ROOT);
    }
    if (lastImage == null) {
      return Stack.GENERIC;
    }
    for (String token : Arrays.asList("node", "nodejs", "npm", "yarn", "pnpm")) {
      if (lastImage.contains(token)) {
        return Stack.NODE;
      }
    }
    for (String token : Arrays.asList("maven", "gradle", "openjdk", "eclipse-temurin", "amazoncorretto", "ibmjava", "jdk", "jre")) {
      if (lastImage.contains(token)) {
        return Stack.JVM;
      }
    }
    for (String token : Arrays.asList("python", "pypy", "conda", "miniconda")) {
      if (lastImage.contains(token)) {
        return Stack.PYTHON;
      }
    }
    return Stack.GENERIC;
  }

  // ---- Issue reporting ------------------------------------------------------

  private static void report(SensorContext context, RuleKey ruleKey, InputFile dockerfile, String message) {
    try {
      NewIssue issue = context.newIssue().forRule(ruleKey);
      NewIssueLocation loc = issue.newLocation()
          .on(dockerfile)
          .at(dockerfile.selectLine(1))
          .message(message);
      issue.at(loc).save();
    } catch (IllegalArgumentException e) {
      LOG.debug("Skipping {} issue on {}: {}", ruleKey, dockerfile, e.getMessage());
    }
  }
}

