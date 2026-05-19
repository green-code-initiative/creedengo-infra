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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.iac.common.api.tree.Tree;
import org.sonar.iac.common.extension.TreeParser;
import org.sonar.iac.common.extension.visitors.InputFileContext;
import org.sonar.iac.common.extension.visitors.MetricsVisitor;
import org.sonar.iac.common.extension.visitors.SensorTelemetryMetrics;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public abstract class AbstractMetricsTest {

  protected NoSonarFilter noSonarFilter = mock(NoSonarFilter.class);
  protected TreeParser<? extends Tree> parser;
  protected String language;
  protected MetricsVisitor visitor;
  protected SensorContextTester sensorContext;
  protected DefaultInputFile inputFile;
  protected FileLinesContext fileLinesContext;
  protected SensorTelemetryMetrics sensorTelemetryMetrics;

  @TempDir
  public File tempFolder;

  @BeforeEach
  void setUp() {
    sensorContext = SensorContextTester.create(tempFolder);
    fileLinesContext = mock(FileLinesContext.class);
    FileLinesContextFactory fileLinesContextFactory = mock(FileLinesContextFactory.class);
    when(fileLinesContextFactory.createFor(any(InputFile.class))).thenReturn(fileLinesContext);
    sensorTelemetryMetrics = spy(new SensorTelemetryMetrics());

    parser = treeParser();
    visitor = metricsVisitor(fileLinesContextFactory);
    language = languageKey();
  }

  protected abstract String languageKey();

  protected abstract TreeParser<? extends Tree> treeParser();

  protected abstract MetricsVisitor metricsVisitor(FileLinesContextFactory fileLinesContextFactory);

  protected MetricsVisitor scan(String code) {
    return scan(code, "file");
  }

  protected MetricsVisitor scan(String code, String filename) {
    inputFile = new TestInputFileBuilder("moduleKey", new File(tempFolder, filename).getName())
      .setCharset(StandardCharsets.UTF_8)
      .setLanguage(language)
      .setContents(code)
      .build();
    var inputFileContext = new InputFileContext(sensorContext, inputFile);
    visitor.scan(inputFileContext, parser.parse(code, inputFileContext));
    return visitor;
  }

  protected void verifyLinesOfCodeMetricsAndTelemetry(Integer... linesOfCode) {
    verifyNCLOCDataMetric(linesOfCode);
    verify(sensorTelemetryMetrics).addLinesOfCode(linesOfCode.length);
  }

  private void verifyNCLOCDataMetric(Integer... linesOfCode) {
    var linesOfCodeSet = Arrays.stream(linesOfCode).collect(Collectors.toSet());
    for (var lineNumber = 1; lineNumber <= inputFile.lines(); lineNumber++) {
      if (linesOfCodeSet.contains(lineNumber)) {
        verify(fileLinesContext).setIntValue(CoreMetrics.NCLOC_DATA_KEY, lineNumber, 1);
      } else {
        verify(fileLinesContext, never()).setIntValue(CoreMetrics.NCLOC_DATA_KEY, lineNumber, 1);
      }
    }
  }
}
