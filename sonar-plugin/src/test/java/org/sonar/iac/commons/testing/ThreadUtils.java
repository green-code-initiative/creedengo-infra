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

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;

public final class ThreadUtils {
  public static List<String> activeCreatedThreadsName() {
    var result = new ArrayList<String>();
    ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    var threads = threadMXBean.dumpAllThreads(true, true);
    for (ThreadInfo threadInfo : threads) {
      if (!threadInfo.isDaemon()) {
        result.add(threadInfo.getThreadName());
      }
    }
    return result;
  }
}
