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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rule.RuleKey;
import org.sonar.check.Rule;
import org.sonar.iac.common.api.checks.CheckContext;
import org.sonar.iac.common.api.checks.IacCheck;
import org.sonar.iac.common.api.checks.InitContext;
import org.sonar.iac.common.api.checks.SecondaryLocation;
import org.sonar.iac.common.api.tree.HasTextRange;
import org.sonar.iac.common.api.tree.Tree;
import org.sonar.iac.common.api.tree.impl.TextRange;
import org.sonar.iac.common.extension.visitors.TreeContext;
import org.sonar.iac.common.extension.visitors.TreeVisitor;

/**
 * Common scaffolding for the per-language creedengo-infra sensors.
 *
 * <p>Sonar-iac 1.24 / 1.46 instantiate their own analysers with hard-coded
 * check lists and do not expose an SPI for third-party plugins to contribute
 * additional {@link IacCheck}s. As a workaround each {@code Infra<Lang>Sensor}
 * re-parses every file of the language using the public sonar-iac parser
 * facade and applies the checks declared by the matching
 * {@code Infra<Lang>CheckRegistrar} whose rule is active in the current
 * quality profile. Issues are reported on the language-specific
 * {@code creedengo-infra-<lang>} repository so they never collide with the
 * upstream rule keys.</p>
 *
 * <p>Subclasses contribute:</p>
 * <ul>
 *   <li>the descriptor (sensor name + Sonar language key + repository key),</li>
 *   <li>the list of annotated check classes,</li>
 *   <li>the parsing function (mapping {@code String} → {@link Tree}).</li>
 * </ul>
 */
abstract class AbstractInfraIacSensor implements Sensor {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractInfraIacSensor.class);

  private final String sensorName;
  private final String languageKey;
  private final String repositoryKey;

  protected AbstractInfraIacSensor(String sensorName, String languageKey, String repositoryKey) {
    this.sensorName = sensorName;
    this.languageKey = languageKey;
    this.repositoryKey = repositoryKey;
  }

  /** Annotated check classes that this sensor will run when active. */
  protected abstract List<Class<?>> checkClasses();

  /** Parse the given file content into a sonar-iac {@link Tree}. */
  protected abstract Tree parse(String content);

  @Override
  public final void describe(SensorDescriptor descriptor) {
    descriptor
        .name(sensorName)
        .onlyOnLanguage(languageKey)
        .createIssuesForRuleRepository(repositoryKey);
  }

  @Override
  public final void execute(SensorContext context) {
    List<ActiveCheck> activeChecks = buildActiveChecks(context);
    if (activeChecks.isEmpty()) {
      LOG.debug("No {} rule active in current profile, skipping {}", repositoryKey, sensorName);
      return;
    }
    Iterable<InputFile> files = context.fileSystem().inputFiles(
        context.fileSystem().predicates().hasLanguage(languageKey));
    for (InputFile inputFile : files) {
      analyzeFile(context, inputFile, activeChecks);
    }
  }

  private List<ActiveCheck> buildActiveChecks(SensorContext context) {
    List<ActiveCheck> result = new ArrayList<>();
    for (Class<?> checkClass : checkClasses()) {
      String ruleKey = ruleKeyOf(checkClass);
      if (ruleKey == null) {
        continue;
      }
      RuleKey key = RuleKey.of(repositoryKey, ruleKey);
      if (context.activeRules().find(key) == null) {
        continue;
      }
      try {
        IacCheck check = (IacCheck) checkClass.getDeclaredConstructor().newInstance();
        result.add(new ActiveCheck(key, check));
      } catch (ReflectiveOperationException e) {
        LOG.warn("Could not instantiate creedengo-infra check {} — skipping", checkClass.getName(), e);
      }
    }
    return result;
  }

  private static String ruleKeyOf(Class<?> checkClass) {
    Rule annotation = checkClass.getAnnotation(Rule.class);
    if (annotation == null || annotation.key().isBlank()) {
      LOG.warn("Check class {} has no @Rule(key=...) annotation — skipping", checkClass.getName());
      return null;
    }
    return annotation.key();
  }

  private void analyzeFile(SensorContext context, InputFile inputFile, List<ActiveCheck> activeChecks) {
    String content;
    try {
      content = inputFile.contents();
    } catch (IOException e) {
      LOG.warn("Could not read {}: {}", inputFile, e.getMessage());
      return;
    }
    Tree root;
    try {
      root = parse(content);
    } catch (RuntimeException e) {
      LOG.debug("Could not parse {}: {}", inputFile, e.getMessage());
      return;
    }
    if (root == null) {
      return;
    }
    for (ActiveCheck active : activeChecks) {
      SensorCheckContext ctx = new SensorCheckContext(context, inputFile, active.ruleKey);
      active.check.initialize(ctx);
      ctx.scan(root);
    }
  }

  // ---- Helpers --------------------------------------------------------------

  private record ActiveCheck(RuleKey ruleKey, IacCheck check) { }

  /**
   * Bridges sonar-iac's {@link InitContext}/{@link CheckContext} APIs with
   * SonarQube's batch sensor API. Mirrors the {@code CollectingContext} used
   * by the local test verifiers but reports to the real {@link SensorContext}
   * instead of an in-memory list.
   */
  private static final class SensorCheckContext extends TreeContext implements InitContext, CheckContext {
    private final TreeVisitor<SensorCheckContext> visitor = new TreeVisitor<>();
    private final SensorContext sensorContext;
    private final InputFile inputFile;
    private final RuleKey ruleKey;

    SensorCheckContext(SensorContext sensorContext, InputFile inputFile, RuleKey ruleKey) {
      this.sensorContext = sensorContext;
      this.inputFile = inputFile;
      this.ruleKey = ruleKey;
    }

    void scan(Tree root) {
      visitor.scan(this, root);
    }

    @Override
    public <T extends Tree> void register(Class<T> cls, BiConsumer<CheckContext, T> consumer) {
      visitor.register(cls, (ctx, node) -> consumer.accept(this, node));
    }

    @Override
    public void reportIssue(TextRange textRange, String message) {
      saveIssue(textRange, message);
    }

    @Override
    public void reportIssue(HasTextRange toHighlight, String message) {
      saveIssue(toHighlight.textRange(), message);
    }

    @Override
    public void reportIssue(HasTextRange toHighlight, String message, SecondaryLocation secondary) {
      // Secondaries reference an InputFile from sonar-iac's own visitor
      // pipeline which is not the InputFile we know about here — drop them.
      saveIssue(toHighlight.textRange(), message);
    }

    @Override
    public void reportIssue(HasTextRange toHighlight, String message, List<SecondaryLocation> secondaries) {
      saveIssue(toHighlight.textRange(), message);
    }

    private void saveIssue(TextRange range, String message) {
      if (range == null) {
        return;
      }
      try {
        NewIssue issue = sensorContext.newIssue().forRule(ruleKey);
        NewIssueLocation loc = issue.newLocation()
            .on(inputFile)
            .at(inputFile.selectLine(range.start().line()))
            .message(Objects.requireNonNullElse(message, ""));
        issue.at(loc).save();
      } catch (IllegalArgumentException e) {
        LOG.debug("Skipping issue on {} (line {}): {}", inputFile, range.start().line(), e.getMessage());
      }
    }
  }
}

