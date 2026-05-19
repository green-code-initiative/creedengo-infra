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
import org.sonar.iac.common.yaml.YamlParser;

/**
 * Plain-YAML verifier for Wave-2B Kubernetes checks. Uses {@link YamlParser}
 * directly (no Helm template processing) and the same {@code (line, message)}
 * comparison strategy as {@link DockerVerifier#verifyIssues(String, IacCheck, DockerVerifier.ExpectedIssue...)}.
 *
 * <p>For Helm-template fixtures (Go-template AST), keep using the existing
 * {@link KubernetesVerifier} which spins up a full {@code KubernetesAnalyzer}.</p>
 */
public final class K8sYamlVerifier {

  public static final Path BASE_DIR = Paths.get("src", "test", "resources", "checks");
  private static final YamlParser PARSER = new YamlParser();

  private K8sYamlVerifier() {
    // utility class
  }

  public static void verifyNoIssue(String relativeFileName, IacCheck check) {
    List<DockerVerifier.RaisedIssue> issues = analyze(relativeFileName, check);
    Assertions.assertThat(issues)
        .as("Expected no issue on %s", relativeFileName)
        .isEmpty();
  }

  public static void verifyIssues(String relativeFileName, IacCheck check, DockerVerifier.ExpectedIssue... expected) {
    List<DockerVerifier.RaisedIssue> actual = analyze(relativeFileName, check);
    List<DockerVerifier.ExpectedIssue> asExpected = actual.stream()
        .map(i -> new DockerVerifier.ExpectedIssue(i.line(), i.message()))
        .sorted(Comparator.comparingInt((DockerVerifier.ExpectedIssue e) -> e.line()).thenComparing(DockerVerifier.ExpectedIssue::message))
        .toList();
    List<DockerVerifier.ExpectedIssue> wantedSorted = Arrays.stream(expected)
        .sorted(Comparator.comparingInt((DockerVerifier.ExpectedIssue e) -> e.line()).thenComparing(DockerVerifier.ExpectedIssue::message))
        .toList();
    Assertions.assertThat(asExpected)
        .as("Mismatch on %s", relativeFileName)
        .containsExactlyElementsOf(wantedSorted);
  }

  public static List<DockerVerifier.RaisedIssue> analyze(String relativeFileName, IacCheck check) {
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
  }

  private static final class CollectingContext extends TreeContext implements InitContext, CheckContext {
    private final TreeVisitor<CollectingContext> visitor = new TreeVisitor<>();
    private final List<DockerVerifier.RaisedIssue> issues = new ArrayList<>();

    void scan(Tree root) {
      visitor.scan(this, root);
    }

    @Override
    public <T extends Tree> void register(Class<T> cls, BiConsumer<CheckContext, T> consumer) {
      visitor.register(cls, (ctx, node) -> consumer.accept(this, node));
    }

    @Override
    public void reportIssue(TextRange textRange, String message) {
      issues.add(new DockerVerifier.RaisedIssue(textRange.start().line(), message));
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
  }
}

