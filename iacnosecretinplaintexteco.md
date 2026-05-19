_(Proposed rule for creedengo-infra — GCI1061.)_

### **Rule title**

No secrets in plain text in IaC (eco-design framing)

### **Rule key**

GCI1061

### **Language and platform**

Terraform / Kubernetes / Helm / CloudFormation / docker-compose

### **Rule description**

Plain-text secrets in IaC are widely-known security smell. The eco-design framing adds a tangible carbon argument: every leaked secret triggers a **rotation event** — re-encrypt, re-deploy across all environments, force-pull updated images. A leak across a 200-service fleet produces hours of CPU·s, GB of registry pulls, and significant operator time. Reference a secret manager (AWS Secrets Manager, GCP Secret Manager, Azure Key Vault, HashiCorp Vault, Sealed Secrets, External Secrets) instead.

_Noncompliant Code Example_

```hcl
resource "aws_db_instance" "main" {
  password = "Sup3rS3cret!"      # plaintext, in Git
}
```

```yaml
apiVersion: v1
kind: Secret
metadata: { name: db }
stringData:
  password: "Sup3rS3cret!"        # plaintext in manifest
```

_Compliant Solution_

```hcl
data "aws_secretsmanager_secret_version" "db_pwd" {
  secret_id = "prod/db/password"
}
resource "aws_db_instance" "main" {
  password = data.aws_secretsmanager_secret_version.db_pwd.secret_string
}
```

### **Rule short description**

Sensitive keys (`password`, `token`, `api_key`, `private_key`, `aws_secret_access_key`, …) must not contain a literal string value in IaC files.

### **Rule justification**

**Why it matters**:
- Security (covered by many rules already), AND
- Eco-design: each leak forces fleet-wide rotation and emergency redeploys.

**Eco-design rationale** (axes 2·5):
- **Buildtime energy**: each rotation event triggers a full pipeline run across N services.
- **Runtime energy**: rolling restart of N replicas across M environments → re-pull, re-init, re-warm-up.

Sources:
- https://docs.aws.amazon.com/secretsmanager/
- https://external-secrets.io/
- https://owasp.org/www-project-secrets-management-cheat-sheet/

Measurement:

| Indicator | Method | Order of magnitude |
| --- | --- | --- |
| Emergency rotations | Git history of secrets in code | aim for 0 |
| Pipeline runs / leak | CI logs | dozens per incident |

### **Severity / Remediation Cost**

Severity: **Major**.

Remediation cost: **Easy** — replace literal with data source.

### **Implementation principle**

- Target tech: TF / K8s / CFN / Helm / docker-compose.
- Detection mechanism: sonar-iac multi-language visitor.
- Algorithm:

  ```
  SENSITIVE_KEYS = {password, secret, token, api_key, private_key, aws_secret_access_key, db_password, ...}
  for each key/value pair in IaC AST:
      if key.lower() in SENSITIVE_KEYS and value is a literal string:
          if value does not start with "${" / "{{" / "!Ref" / "data." / "vault:": report(value, "Use a secret manager reference")
  ```

- Known false positives: dev fixtures and test manifests — exclude under `tests/`, `fixtures/`.
- Feasibility verdict: ✅
- Alternative tooling: gitleaks, trufflehog, Checkov; SonarSource `S6648`/`S6648` family overlap.
