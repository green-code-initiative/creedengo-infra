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

import org.sonar.api.Plugin;

/**
 * SonarQube plugin entry point for creedengo-infra.
 * <p>
 * Registers, for each supported IaC language (Docker, Kubernetes/Helm,
 * Terraform, CloudFormation, YAML/CI):
 * <ul>
 *   <li>one {@code RulesDefinition} (so the rules appear in the SonarQube rule
 *       repository),</li>
 *   <li>one {@code BuiltInQualityProfile} ("creedengo way") activating those
 *       rules,</li>
 *   <li>the {@link org.sonar.iac.common.api.checks.IacCheck} classes contributed
 *       by the matching {@code Infra<Lang>CheckRegistrar} as plugin extensions.</li>
 * </ul>
 * <p>
 * <strong>Execution status (2026-05).</strong> sonar-iac 1.24 / 1.46 instantiate
 * their analyser ({@code KubernetesAnalyzer}, {@code DockerSensor}, …) with a
 * hard-coded list of visitors built from each language's {@code CheckList};
 * there is no public SPI for a third-party plugin to contribute additional
 * {@code IacCheck} instances to that list. As a workaround, this plugin ships
 * its own per-language {@link org.sonar.api.batch.sensor.Sensor} that
 * re-parses each file using the public sonar-iac parser facade
 * ({@link org.sonar.iac.docker.parser.DockerParser},
 * {@link org.sonar.iac.common.yaml.YamlParser},
 * {@link org.sonar.iac.terraform.parser.HclParser}) and runs every check
 * declared by the matching {@code Infra<Lang>CheckRegistrar} whose rule is
 * active in the current quality profile. The extra parse pass is cheap (IaC
 * files are small) and our issues land on dedicated repositories
 * ({@code creedengo-infra-docker}, {@code creedengo-infra-kubernetes}, …) so
 * they never collide with the upstream {@code docker} / {@code kubernetes} /
 * … rule keys.
 * </p>
 * <p>
 * Helm template processing (Go-template AST) is intentionally <em>not</em>
 * routed through {@link InfraKubernetesSensor}; the plain {@link YamlParser}
 * is used instead, so checks that match on Helm-only AST nodes (e.g.
 * {@code UseOfProbesCheck} listening on {@code CommandNode}) will not fire
 * on Helm manifests at scan time. A dedicated {@code KubernetesAnalyzer}-based
 * sensor is tracked as a follow-up — see {@code doc/IMPORT-NOTES.md} §7.
 * </p>
 *
 * @see <a href="../../doc/IMPORT-NOTES.md">doc/IMPORT-NOTES.md §7</a> for the
 *      remaining Wave-3 follow-ups (Helm AST, cross-file sensors).
 */
public class InfraPlugin implements Plugin {

  @Override
  public void define(Context context) {
    // Server-side rule repositories (one per IaC language).
    context.addExtension(InfraDockerRulesDefinition.class);
    context.addExtension(InfraKubernetesRulesDefinition.class);
    context.addExtension(InfraTerraformRulesDefinition.class);
    context.addExtension(InfraCloudFormationRulesDefinition.class);
    context.addExtension(InfraYamlRulesDefinition.class);

    // Built-in "creedengo way" quality profiles (one per IaC language).
    context.addExtension(InfraDockerQualityProfile.class);
    context.addExtension(InfraKubernetesQualityProfile.class);
    context.addExtension(InfraTerraformQualityProfile.class);
    context.addExtension(InfraCloudFormationQualityProfile.class);
    context.addExtension(InfraYamlQualityProfile.class);

    // Scan-time execution — one Sensor per IaC language. Each Sensor reads
    // its checks from the matching Infra<Lang>CheckRegistrar and reports on
    // its dedicated rule repository.
    context.addExtension(InfraDockerSensor.class);
    context.addExtension(InfraKubernetesSensor.class);
    context.addExtension(InfraTerraformSensor.class);
    context.addExtension(InfraCloudFormationSensor.class);
    context.addExtension(InfraYamlSensor.class);

    // Cross-file / non-AST sensors (audit sibling files such as .dockerignore).
    context.addExtension(DockerignoreSensor.class);
    context.addExtension(K8sWorkloadCrossRefSensor.class);
    context.addExtension(HelmValuesEcoSensor.class);
  }
}

