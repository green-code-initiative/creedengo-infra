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
package org.sonar.iac.commons.testing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.error.AnalysisError;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.batch.sensor.issue.IssueLocation;
import org.sonar.api.config.internal.MapSettings;

public abstract class ExtensionSensorTest extends AbstractSensorTest {

  protected static final String PARSING_ERROR_RULE_KEY = "S2260";

  protected abstract InputFile emptyFile();

  protected abstract InputFile fileWithParsingError();

  protected abstract Map<InputFile, Integer> validFilesMappedToExpectedLoCs();

  protected abstract InputFile validFile();

  protected abstract void verifyDebugMessages(List<String> logs);

  @Test
  void emptyFileShouldRaiseNoIssue() {
    analyze(sensor(checkFactory(PARSING_ERROR_RULE_KEY)), emptyFile());
    assertThat(context.allIssues()).isEmpty();
    assertThat(context.allAnalysisErrors()).isEmpty();
    verifyLinesOfCodeTelemetry(0);
  }

  @Test
  protected void shouldRaiseIssueOnParsingErrorWhenIssueActive() {
    InputFile inputFile = fileWithParsingError();
    analyze(sensor(checkFactory(PARSING_ERROR_RULE_KEY)), inputFile);

    // Test issue
    assertThat(context.allIssues()).hasSize(1);
    Issue issue = context.allIssues().iterator().next();
    assertThat(issue.ruleKey().rule()).as("A parsing error must be raised").isEqualTo(PARSING_ERROR_RULE_KEY);
    assertThat(issue.ruleKey().repository()).as("A parsing error must be raised").isEqualTo(repositoryKey());
    IssueLocation location = issue.primaryLocation();
    assertThat(location.inputComponent()).isEqualTo(inputFile);
    assertThat(location.message()).isEqualTo("A parsing error occurred in this file.");
    assertThat(issue.primaryLocation().textRange()).isNotNull();

    // Test analysis warning
    Collection<AnalysisError> analysisErrors = context.allAnalysisErrors();
    assertThat(analysisErrors).hasSize(1);
    AnalysisError analysisError = analysisErrors.iterator().next();
    assertThat(analysisError.inputFile()).isEqualTo(inputFile);
    assertThat(analysisError.message()).startsWith("Unable to parse file:");
  }

  @Test
  void shouldRaiseNoIssueOnParsingErrorWhenIssueInactive() {
    analyze(sensor(checkFactory()), fileWithParsingError());
    assertThat(context.allIssues()).isEmpty();
    assertThat(context.allAnalysisErrors()).hasSize(1);
    verifyLinesOfCodeTelemetry(0);
  }

  @Test
  void shouldRaiseNoIssueWhenSensorInactive() {
    MapSettings settings = new MapSettings();
    settings.setProperty(getActivationSettingKey(), false);
    context.setSettings(settings);

    analyze(sensor(checkFactory(PARSING_ERROR_RULE_KEY)), fileWithParsingError());
    assertThat(context.allIssues()).isEmpty();
    assertThat(context.allAnalysisErrors()).isEmpty();
    verifyLinesOfCodeTelemetry(0);
  }

  @Test
  protected void shouldRaiseIssueWhenFileCorrupted() throws IOException {
    InputFile inputFile = validFile();
    InputFile spyInputFile = spy(inputFile);
    when(spyInputFile.contents()).thenThrow(IOException.class);
    analyze(spyInputFile);

    Collection<AnalysisError> analysisErrors = context.allAnalysisErrors();
    assertThat(analysisErrors).hasSize(1);
    AnalysisError analysisError = analysisErrors.iterator().next();
    assertThat(analysisError.inputFile()).isEqualTo(spyInputFile);
    assertThat(analysisError.message()).startsWith("Unable to parse file:");
    assertThat(analysisError.location()).isNull();
    verifyLinesOfCodeTelemetry(0);
  }

  @Test
  protected void shouldCorrectlyAddLinesOfCodesInAnalyzerTelemetryMetrics() {
    var totalLinesOfCode = validFilesMappedToExpectedLoCs().values().stream().mapToInt(Integer::intValue).sum();
    analyze(validFilesMappedToExpectedLoCs().keySet().toArray(new InputFile[0]));
    verifyLinesOfCodeTelemetry(totalLinesOfCode);
  }

}
