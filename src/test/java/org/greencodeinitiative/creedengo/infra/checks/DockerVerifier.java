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
package org.greencodeinitiative.creedengo.infra.checks;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import org.assertj.core.api.Assertions;
import org.sonar.iac.common.api.checks.CheckContext;
import org.sonar.iac.common.api.checks.IacCheck;
import org.sonar.iac.common.api.checks.InitContext;
import org.sonar.iac.common.api.checks.SecondaryLocation;
import org.sonar.iac.common.api.tree.HasTextRange;
import org.sonar.iac.common.api.tree.Tree;
import org.sonar.iac.common.api.tree.impl.TextRange;
import org.sonar.iac.common.extension.visitors.TreeContext;
import org.sonar.iac.common.extension.visitors.TreeVisitor;
import org.sonar.iac.commons.testing.Verifier;
import org.sonar.iac.docker.parser.DockerParser;

/**
 * Thin verifier wrapper for sonar-iac-docker checks. Mirrors the pattern of
 * {@link KubernetesVerifier} (simpler — Docker has no Helm/cross-file context
 * yet for Wave-2B rules).
 *
 * <p>Fixtures are resolved relative to {@code src/test/resources/checks/<CheckName>/}.
 * The {@link #verifyIssues(String, IacCheck, ExpectedIssue...)} helper compares
 * issues by {@code (line, message)} only — column offsets are noisy across
 * sonar-iac versions and the Wave-2B rules report on whole tokens. The strict
 * {@link Verifier#verify} path remains available via {@link #verifyStrict}.</p>
 */
public final class DockerVerifier {
/**
  public static final Path BASE_DIR = Paths.get("src", "test", "resources", "checks");
  private static final DockerParser PARSER = DockerParser.create();

  private DockerVerifier() {
    // utility class
  }

  public static void verifyNoIssue(String relativeFileName, IacCheck check) {
    List<RaisedIssue> issues = analyze(relativeFileName, check);
    Assertions.assertThat(issues)
      .as("Expected no issue on %s", relativeFileName)
      .isEmpty();
  }
**/
  /**
   * Assert that the given check raises exactly the expected (line, message)
   * pairs on the fixture. Order-insensitive.
   */
  /**public static void verifyIssues(String relativeFileName, IacCheck check, ExpectedIssue... expected) {
    List<RaisedIssue> actual = analyze(relativeFileName, check);
    List<ExpectedIssue> asExpected = actual.stream()
      .map(i -> new ExpectedIssue(i.line, i.message))
      .sorted(Comparator.comparingInt((ExpectedIssue e) -> e.line).thenComparing(e -> e.message))
      .toList();
    List<ExpectedIssue> wantedSorted = Arrays.stream(expected)
      .sorted(Comparator.comparingInt((ExpectedIssue e) -> e.line).thenComparing(e -> e.message))
      .toList();
    Assertions.assertThat(asExpected)
      .as("Mismatch on %s", relativeFileName)
      .containsExactlyElementsOf(wantedSorted);
  }**/

  /** Strict {@link Verifier} path (positional, with Noncompliant comments). */
  /**public static void verifyStrict(String relativeFileName, IacCheck check, Verifier.Issue... expected) {
    Verifier.verify(PARSER, BASE_DIR.resolve(relativeFileName), check, expected);
  }**/

  /** Run the check and return raised issues as plain (line, message) tuples. */
  /**public static List<RaisedIssue> analyze(String relativeFileName, IacCheck check) {
    Path path = BASE_DIR.resolve(relativeFileName);
    String content;
    try {
      content = Files.readString(path);
    } catch (Exception e) {
      throw new IllegalStateException("Cannot read fixture " + path, e);
    }
    Tree root = PARSER.parse(content, null);
    CollectingContext ctx = new CollectingContext();
    check.initialize(ctx);
    ctx.scan(root);
    return ctx.issues;
  }**/

  // ---- Helpers --------------------------------------------------------------

  /**public record ExpectedIssue(int line, String message) {
    public static ExpectedIssue at(int line, String message) {
      return new ExpectedIssue(line, message);
    }
  }

  public record RaisedIssue(int line, String message) {
  }

  private static final class CollectingContext extends TreeContext implements InitContext, CheckContext {
    private final TreeVisitor<CollectingContext> visitor = new TreeVisitor<>();
    private final List<RaisedIssue> issues = new ArrayList<>();

    void scan(Tree root) {
      visitor.scan(this, root);
    }

    @Override
    public <T extends Tree> void register(Class<T> cls, BiConsumer<CheckContext, T> consumer) {
      visitor.register(cls, (ctx, node) -> consumer.accept(this, node));
    }

    @Override
    public void reportIssue(TextRange textRange, String message) {
      issues.add(new RaisedIssue(textRange.start().line(), message));
    }

    @Override
    public void reportIssue(HasTextRange toHighlight, String message) {
      reportIssue(toHighlight.textRange(), message);
    }

    @Override
    public void reportIssue(HasTextRange toHighlight, String message, SecondaryLocation secondary) {
      reportIssue(toHighlight.textRange(), message);
    }

    @Override
    public void reportIssue(HasTextRange toHighlight, String message, List<SecondaryLocation> secondaries) {
      reportIssue(toHighlight.textRange(), message);
    }

    // Required by record equals/hashCode in older sonar-iac TreeContext — no-op.
    @SuppressWarnings("unused")
    private static int hash(Object o) {
      return Objects.hashCode(o);
    }
  }**/
}

