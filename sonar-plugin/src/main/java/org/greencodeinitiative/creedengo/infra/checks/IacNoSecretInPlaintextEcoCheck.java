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

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import org.sonar.check.Rule;
import org.sonar.iac.common.api.checks.CheckContext;
import org.sonar.iac.common.api.checks.IacCheck;
import org.sonar.iac.common.api.checks.InitContext;
import org.sonar.iac.common.yaml.tree.MappingTree;
import org.sonar.iac.common.yaml.tree.TupleTree;

/**
 * GCI1061 — flag IaC files declaring sensitive values as plain-text
 * literals rather than as a reference to a secret manager.
 *
 * <p>Eco-design framing: every leaked secret triggers a full fleet rotation
 * (re-encrypt, re-deploy across every environment) — hours of pipeline CPU·s,
 * GB of registry pulls. See {@code iacnosecretinplaintexteco.md}.</p>
 *
 * <p>Scope: any YAML tree (Kubernetes {@code Secret}, {@code ConfigMap},
 * Helm {@code values.yaml}, docker-compose, CloudFormation Parameters).
 * The check visits every {@link MappingTree} and reports tuples where the
 * key matches a known sensitive name and the value is a literal string
 * without a secret-reference prefix.</p>
 */
@Rule(key = "GCI1061")
public class IacNoSecretInPlaintextEcoCheck implements IacCheck {

  /** Case-insensitive key names that hold secrets. */
  private static final Set<String> SENSITIVE_KEYS = Set.of(
      "password", "passwd", "pwd",
      "secret", "secrets",
      "token", "access_token", "auth_token",
      "api_key", "apikey", "api-key",
      "private_key", "privatekey", "private-key",
      "aws_secret_access_key", "aws_access_key_id",
      "db_password", "dbpassword", "db-password",
      "client_secret", "clientsecret", "client-secret");

  /**
   * Prefixes that prove the value is a reference (variable, function, secret manager).
   * Anything starting with one of these is considered compliant.
   */
  private static final List<Pattern> REFERENCE_PREFIXES = List.of(
      Pattern.compile("^\\$\\{.*}$"),                 // ${VAR}
      Pattern.compile("^\\$\\{\\{.*}}$"),              // ${{ secrets.X }}
      Pattern.compile("^\\{\\{.*}}$"),                 // {{ .Values.x }}
      Pattern.compile("^!Ref\\s+.*", Pattern.CASE_INSENSITIVE),
      Pattern.compile("^!Sub\\s+.*", Pattern.CASE_INSENSITIVE),
      Pattern.compile("^!ImportValue\\s+.*", Pattern.CASE_INSENSITIVE),
      Pattern.compile("^!GetAtt\\s+.*", Pattern.CASE_INSENSITIVE),
      Pattern.compile("^data\\..*", Pattern.CASE_INSENSITIVE),
      Pattern.compile("^var\\..*", Pattern.CASE_INSENSITIVE),
      Pattern.compile("^vault:.*", Pattern.CASE_INSENSITIVE),
      Pattern.compile("^valueFrom:.*", Pattern.CASE_INSENSITIVE),
      Pattern.compile("^null$|^~$", Pattern.CASE_INSENSITIVE));

  /** Marker that turns the rule off line-locally (kept for parity with other checks). */
  private static final Pattern OPT_OUT = Pattern.compile("(?i)eco-design\\s*:\\s*ignore");

  @Override
  public void initialize(@Nonnull InitContext init) {
    init.register(MappingTree.class, IacNoSecretInPlaintextEcoCheck::check);
  }

  private static void check(CheckContext ctx, MappingTree mapping) {
    for (TupleTree tuple : mapping.elements()) {
      String key = KubernetesCheckUtils.asScalarString(tuple.key()).orElse("");
      if (!SENSITIVE_KEYS.contains(key.toLowerCase(Locale.ROOT))) {
        continue;
      }
      String value = KubernetesCheckUtils.asScalarString(tuple.value()).orElse(null);
      if (value == null) {
        // value is a mapping / sequence (e.g. valueFrom) — assumed safe.
        continue;
      }
      String trimmed = value.trim();
      if (trimmed.isEmpty() || OPT_OUT.matcher(trimmed).find()) {
        continue;
      }
      if (isReference(trimmed)) {
        continue;
      }
      ctx.reportIssue(tuple,
          "Sensitive key '" + key + "' has a plain-text value — reference a secret manager (Vault, AWS/GCP/Azure Secret Manager, External Secrets) instead.");
    }
  }

  /** Visible for tests. */
  static boolean isReference(String value) {
    return REFERENCE_PREFIXES.stream().anyMatch(p -> p.matcher(value).matches());
  }
}
