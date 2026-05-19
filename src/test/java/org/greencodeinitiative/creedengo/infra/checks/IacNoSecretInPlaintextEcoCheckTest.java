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
import static org.greencodeinitiative.creedengo.infra.checks.DockerVerifier.ExpectedIssue.at;

import org.junit.jupiter.api.Test;

class IacNoSecretInPlaintextEcoCheckTest {

  @Test
  void compliant() {
    K8sYamlVerifier.verifyNoIssue("IacNoSecretInPlaintextEcoCheck/compliant.yml",
        new IacNoSecretInPlaintextEcoCheck());
  }

  @Test
  void noncompliant() {
    K8sYamlVerifier.verifyIssues("IacNoSecretInPlaintextEcoCheck/noncompliant.yml",
        new IacNoSecretInPlaintextEcoCheck(),
        at(6,  "Sensitive key 'password' has a plain-text value — reference a secret manager (Vault, AWS/GCP/Azure Secret Manager, External Secrets) instead."),
        at(7,  "Sensitive key 'api_key' has a plain-text value — reference a secret manager (Vault, AWS/GCP/Azure Secret Manager, External Secrets) instead."),
        at(14, "Sensitive key 'DB_PASSWORD' has a plain-text value — reference a secret manager (Vault, AWS/GCP/Azure Secret Manager, External Secrets) instead."),
        at(15, "Sensitive key 'TOKEN' has a plain-text value — reference a secret manager (Vault, AWS/GCP/Azure Secret Manager, External Secrets) instead."));
  }

  @Test
  void isReference_truthTable() {
    assertThat(IacNoSecretInPlaintextEcoCheck.isReference("${DB_PASSWORD}")).isTrue();
    assertThat(IacNoSecretInPlaintextEcoCheck.isReference("{{ .Values.x }}")).isTrue();
    assertThat(IacNoSecretInPlaintextEcoCheck.isReference("vault:secret/data/app#token")).isTrue();
    assertThat(IacNoSecretInPlaintextEcoCheck.isReference("data.aws_secretsmanager_secret_version.x.secret_string")).isTrue();
    assertThat(IacNoSecretInPlaintextEcoCheck.isReference("Sup3rS3cret!")).isFalse();
  }
}

