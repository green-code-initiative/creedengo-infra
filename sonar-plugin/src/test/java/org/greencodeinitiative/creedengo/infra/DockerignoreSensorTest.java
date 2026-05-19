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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.rule.internal.NewActiveRule;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.rule.RuleKey;

/** Unit tests for {@link DockerignoreSensor} (GCI1028). */
class DockerignoreSensorTest {

  private static final String DOCKERFILE_NODE =
      """
      FROM node:20-alpine
      WORKDIR /app
      COPY . .
      CMD ["node", "server.js"]
      """;

  private static final String DOCKERFILE_MAVEN_MULTISTAGE =
      """
      FROM maven:3.9-eclipse-temurin-21 AS build
      WORKDIR /src
      COPY . .
      RUN mvn -B package

      FROM eclipse-temurin:21-jre
      COPY --from=build /src/target/app.jar /app.jar
      CMD ["java", "-jar", "/app.jar"]
      """;

  @Test
  void shouldReportMissingDockerignoreFile(@TempDir Path baseDir) {
    SensorContextTester context = newContext(baseDir);
    InputFile dockerfile = addDockerfile(context, baseDir, "Dockerfile", DOCKERFILE_NODE);

    new DockerignoreSensor().execute(context);

    assertThat(context.allIssues()).hasSize(1);
    Issue issue = context.allIssues().iterator().next();
    assertThat(issue.ruleKey())
        .isEqualTo(RuleKey.of(InfraDockerRulesDefinition.REPOSITORY_KEY, "GCI1028"));
    assertThat(issue.primaryLocation().inputComponent()).isEqualTo(dockerfile);
    assertThat(issue.primaryLocation().message()).contains(".dockerignore");
  }

  @Test
  void shouldReportMissingEntriesForNodeStack(@TempDir Path baseDir) throws IOException {
    SensorContextTester context = newContext(baseDir);
    addDockerfile(context, baseDir, "Dockerfile", DOCKERFILE_NODE);
    // .dockerignore is present but missing node_modules and .env.
    writeFile(baseDir.resolve(".dockerignore"), ".git\n");

    new DockerignoreSensor().execute(context);

    assertThat(context.allIssues()).hasSize(1);
    String message = context.allIssues().iterator().next().primaryLocation().message();
    assertThat(message).contains("Node.js").contains("node_modules").contains(".env");
  }

  @Test
  void shouldNotReportWhenAllRequiredEntriesPresent(@TempDir Path baseDir) throws IOException {
    SensorContextTester context = newContext(baseDir);
    addDockerfile(context, baseDir, "Dockerfile", DOCKERFILE_MAVEN_MULTISTAGE);
    writeFile(baseDir.resolve(".dockerignore"),
        "target/\n.git\n.env\n.idea/\n");

    new DockerignoreSensor().execute(context);

    assertThat(context.allIssues()).isEmpty();
  }

  @Test
  void shouldFallBackToParentDockerignore(@TempDir Path baseDir) throws IOException {
    SensorContextTester context = newContext(baseDir);
    // Monorepo: Dockerfile lives under services/web, .dockerignore at the root.
    Path subdir = baseDir.resolve("services").resolve("web");
    Files.createDirectories(subdir);
    addDockerfile(context, baseDir, "services/web/Dockerfile", DOCKERFILE_NODE);
    writeFile(baseDir.resolve(".dockerignore"), "node_modules\n.git\n.env\n");

    new DockerignoreSensor().execute(context);

    assertThat(context.allIssues()).isEmpty();
  }

  @Test
  void shouldNotRunWhenRuleIsInactive(@TempDir Path baseDir) {
    SensorContextTester context = SensorContextTester.create(baseDir);
    context.setActiveRules(new ActiveRulesBuilder().build());
    addDockerfile(context, baseDir, "Dockerfile", DOCKERFILE_NODE);

    new DockerignoreSensor().execute(context);

    assertThat(context.allIssues()).isEmpty();
  }

  @Test
  void shouldInferStackFromLastFromInstruction() {
    assertThat(DockerignoreSensor.inferStackFromContent("FROM node:20\nCMD [\"node\"]"))
        .isEqualTo(DockerignoreSensor.Stack.NODE);
    assertThat(DockerignoreSensor.inferStackFromContent(
        "FROM maven:3.9 AS build\nFROM eclipse-temurin:21-jre\n"))
        .isEqualTo(DockerignoreSensor.Stack.JVM);
    assertThat(DockerignoreSensor.inferStackFromContent("FROM python:3.12-slim"))
        .isEqualTo(DockerignoreSensor.Stack.PYTHON);
    assertThat(DockerignoreSensor.inferStackFromContent("FROM alpine:3.20"))
        .isEqualTo(DockerignoreSensor.Stack.GENERIC);
    assertThat(DockerignoreSensor.inferStackFromContent("# no FROM here"))
        .isEqualTo(DockerignoreSensor.Stack.GENERIC);
  }

  // ---- Helpers --------------------------------------------------------------

  private static SensorContextTester newContext(Path baseDir) {
    SensorContextTester context = SensorContextTester.create(baseDir);
    ActiveRulesBuilder rules = new ActiveRulesBuilder();
    rules.addRule(new NewActiveRule.Builder()
        .setRuleKey(RuleKey.of(InfraDockerRulesDefinition.REPOSITORY_KEY, "GCI1028"))
        .setName("GCI1028")
        .build());
    context.setActiveRules(rules.build());
    return context;
  }

  private static InputFile addDockerfile(SensorContextTester context, Path baseDir,
      String relativePath, String content) {
    InputFile file = new TestInputFileBuilder("moduleKey", relativePath)
        .setModuleBaseDir(baseDir)
        .setType(InputFile.Type.MAIN)
        .setLanguage(InfraDockerRulesDefinition.LANGUAGE)
        .setCharset(StandardCharsets.UTF_8)
        .setContents(content)
        .build();
    context.fileSystem().add(file);
    return file;
  }

  private static void writeFile(Path path, String content) throws IOException {
    Files.createDirectories(path.getParent());
    Files.writeString(path, content, StandardCharsets.UTF_8);
  }
}

