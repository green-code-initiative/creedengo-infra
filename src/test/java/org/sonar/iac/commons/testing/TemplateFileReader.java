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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TemplateFileReader {

  public static final Path BASE_DIR = Paths.get("src", "test", "resources", "checks");

  public static String readContent(String path) {
    try {
      return Files.readString(BASE_DIR.resolve(path));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static String readTemplateAndReplace(String path, String type) {
    String content = readContent(path);
    if (!content.contains("${type}")) {
      throw new RuntimeException("No variable \"${type}\" to replace found in the following template: " + path);
    }
    return content.replace("${type}", type);
  }

  public static String readTemplateAndReplace(String path, String... types) {
    if (types.length % 2 == 1) {
      throw new RuntimeException("There should be even number of strings");
    }
    String content = readContent(path);
    for (int i = 0; i < types.length; i = i + 2) {
      if (!content.contains(types[i])) {
        throw new RuntimeException("No variable \"" + types[i] + "\" to replace found in the following template: " + path);
      }
      content = content.replace(types[i], types[i + 1]);
    }
    return content;
  }

}
