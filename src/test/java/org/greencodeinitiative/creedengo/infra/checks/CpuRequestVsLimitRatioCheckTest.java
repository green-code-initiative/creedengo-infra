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

import static org.assertj.core.api.Assertions.assertThat;
//import static org.greencodeinitiative.creedengo.infra.checks.DockerVerifier.ExpectedIssue.at;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class CpuRequestVsLimitRatioCheckTest {

  @Test
  void compliant() {
    K8sYamlVerifier.verifyNoIssue("CpuRequestVsLimitRatioCheck/compliant.yaml",
        new CpuRequestVsLimitRatioCheck());
  }
/**
  @Test
  void noncompliant() {
    // cpu-burst (line 7): cpu ratio = 2000/100 = 20× → cpu issue.
    // mem-burst (line 16): memory ratio = 1Gi / 64Mi = 16× → memory issue.
    K8sYamlVerifier.verifyIssues("CpuRequestVsLimitRatioCheck/noncompliant.yaml",
        new CpuRequestVsLimitRatioCheck(),
        at(7,  "Ratio cpu limits/requests is 20.0x (max 4.0x): rightsize to avoid throttling and HPA flapping."),
        at(16, "Ratio memory limits/requests is 16.0x (max 4.0x): rightsize to avoid throttling and HPA flapping."));
  }

  @Test
  void parseQuantity_cpu() {
    assertThat(CpuRequestVsLimitRatioCheck.parseQuantity("cpu", "100m")).contains(0.1);
    assertThat(CpuRequestVsLimitRatioCheck.parseQuantity("cpu", "2")).contains(2.0);
    assertThat(CpuRequestVsLimitRatioCheck.parseQuantity("cpu", "abc")).isEqualTo(Optional.empty());
  }

  @Test
  void parseQuantity_memory() {
    assertThat(CpuRequestVsLimitRatioCheck.parseQuantity("memory", "128Mi")).contains(128.0 * 1024 * 1024);
    assertThat(CpuRequestVsLimitRatioCheck.parseQuantity("memory", "1Gi")).contains(1024.0 * 1024 * 1024);
    assertThat(CpuRequestVsLimitRatioCheck.parseQuantity("memory", "500M")).contains(500.0 * 1_000_000);
    assertThat(CpuRequestVsLimitRatioCheck.parseQuantity("memory", "junk")).isEqualTo(Optional.empty());
  }**/
}

