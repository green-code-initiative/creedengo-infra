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

import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.sonar.api.Plugin;
import org.sonar.api.SonarRuntime;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractExtensionTest {
  protected abstract SonarRuntime sonarQubeRuntime();

  protected abstract SonarRuntime sonarLintRuntime();

  protected abstract Consumer<Plugin.Context> extensionDefiner();

  protected abstract int extensionsCountOnSQ();

  protected int extensionsCountOnSL() {
    return extensionsCountOnSQ();
  }

  @Test
  void shouldDefineExtensionsOnSonarqube() {
    var runtime = sonarQubeRuntime();
    var context = new Plugin.Context(runtime);

    extensionDefiner().accept(context);

    assertThat(context.getExtensions())
      .hasSize(extensionsCountOnSQ());
  }

  @Test
  void shouldDefineExtensionsOnSonarlint() {
    var runtime = sonarLintRuntime();
    var context = new Plugin.Context(runtime);

    extensionDefiner().accept(context);

    assertThat(context.getExtensions())
      .hasSize(extensionsCountOnSL());
  }
}
