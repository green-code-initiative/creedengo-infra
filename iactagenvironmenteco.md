_(Proposed rule for creedengo-infra — 1062.)_

### **Rule title**

Tag every cloud resource with `environment` and `owner`

### **Rule key**

1062

### **Language and platform**

Terraform / CloudFormation / docker-compose / Kubernetes labels

### **Rule description**

Eco-design depends on **observability**: you cannot reduce what you cannot attribute. Tagging every resource with at least `environment` (prod/staging/dev) and `owner` (team) unlocks: rightsizing reports per team, scheduled scale-down of non-prod (`1055`, `1064`), Cloud Carbon Footprint per business unit, automated cleanup of orphan resources.

_Noncompliant Code Example_

```hcl
resource "aws_instance" "web" {
  instance_type = "m7g.large"
  ami           = data.aws_ami.al.id
  # No tags.
}
```

_Compliant Solution_

```hcl
resource "aws_instance" "web" {
  instance_type = "m7g.large"
  ami           = data.aws_ami.al.id
  tags = {
    environment = "production"
    owner       = "team-payments"
    cost-center = "PAY-001"
    creedengo   = "audited"
  }
}
```

### **Rule short description**

Every taggable cloud resource must declare at least `environment` and `owner` tags / labels.

### **Rule justification**

**Why it matters**:
- Without tags, FinOps and GreenOps reports cannot break costs/emissions down by team or environment.
- Cleanup automation (Janitor Monkey, AWS Resource Groups + Lambda) relies on tags to identify safe deletion candidates.

**Eco-design rationale** (axes 5·6·7):
- **Provisioning efficiency**: enables team-specific rightsizing.
- **Scale-to-fit**: enables environment-aware scheduling (e.g., scale dev down at night).
- **Storage footprint**: orphaned untagged resources can be safely deleted.

Sources:
- https://aws.amazon.com/architecture/well-architected/sustainability/
- https://www.finops.org/
- https://greensoftware.foundation/articles/sci-specification

Measurement:

| Indicator | Method | Order of magnitude |
| --- | --- | --- |
| Untagged resources | provider tag report | aim for 0 |
| Cost-attribution coverage | FinOps dashboard | 100 % |

### **Severity / Remediation Cost**

Severity: **Info**.

Remediation cost: **Easy** — add 4 lines per resource.

### **Implementation principle**

- Target tech: TF / CFN / K8s.
- Detection mechanism: sonar-iac visitor.
- Algorithm:

  ```
  REQUIRED_TAGS = property("creedengo.infra.requiredTags", default=["environment","owner"])
  for each taggable resource:
      for t in REQUIRED_TAGS:
          if t not in resource.tags (case-insensitive): report(resource, "Missing tag " + t)
  ```

- Known false positives: resource types that do not support tags — exclude via known list.
- Feasibility verdict: ✅
- Alternative tooling: AWS Tag Policy; Azure Policy `tags`; OPA Gatekeeper.
