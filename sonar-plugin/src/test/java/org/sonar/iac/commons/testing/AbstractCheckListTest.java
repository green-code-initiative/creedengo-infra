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
import java.util.Collection;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.junit.jupiter.api.Test;
import org.sonar.iac.common.checks.ParsingErrorCheck;
import org.sonar.iac.common.checks.ToDoCommentCheck;

import static org.apache.commons.io.filefilter.FileFilterUtils.and;
import static org.apache.commons.io.filefilter.FileFilterUtils.notFileFilter;
import static org.apache.commons.io.filefilter.FileFilterUtils.prefixFileFilter;
import static org.apache.commons.io.filefilter.FileFilterUtils.suffixFileFilter;
import static org.apache.commons.io.filefilter.FileFilterUtils.trueFileFilter;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public abstract class AbstractCheckListTest {

  protected abstract List<Class<?>> checks();

  protected abstract File checkClassDir();

  protected boolean hasTodoCommentCheck() {
    return true;
  }

  protected boolean hasParsingFailureCheck() {
    return true;
  }

  @Test
  void containsParsingErrorCheck() {
    assumeTrue(hasParsingFailureCheck());
    assertThat(checks()).contains(ParsingErrorCheck.class);
  }

  @Test
  void containsToDoCommentCheck() {
    assumeTrue(hasTodoCommentCheck());
    assertThat(checks()).contains(ToDoCommentCheck.class);
  }

  /**
   * Enforces that each check is declared in the list.
   */
  @Test
  protected void count() {
    IOFileFilter filter = and(suffixFileFilter("Check.java"), notFileFilter(prefixFileFilter("Abstract")));
    Collection<File> files = FileUtils.listFiles(checkClassDir(), filter, trueFileFilter());
    // We can increase the files size by 2 because the ParsingErrorCheck and ToDoCommentCheck are located in iac-commons
    int checksSize = files.size();
    if (hasTodoCommentCheck()) {
      checksSize++;
    }
    if (hasParsingFailureCheck()) {
      checksSize++;
    }
    assertThat(checks()).hasSize(checksSize);
  }

  /**
   * Enforces that each check has a test
   */
  @Test
  void test() {
    for (Class<?> cls : checks()) {
      // Exception on class from the common package
      if (!cls.getName().contains("iac.common.checks")) {
        String testName = '/' + cls.getName().replace('.', '/') + "Test.class";
        assertThat(getClass().getResource(testName))
          .overridingErrorMessage("No test for " + cls.getSimpleName())
          .isNotNull();
      }
    }
  }
}
