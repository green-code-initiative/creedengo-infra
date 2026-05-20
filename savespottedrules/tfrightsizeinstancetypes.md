_(Proposed rule for creedengo-infra — 1051.)_

### **Rule title**

Avoid over-sized instance types ("xl" defaults)

### **Rule key**

1051

### **Language and platform**

Terraform (AWS, GCP, Azure)

### **Rule description**

The most common eco-design anti-pattern in IaC is the "safe default": engineers pick `m5.2xlarge` or `n2-standard-8` "just in case". Combined with autoscaling absence, this leads to clusters of large, idle VMs. The rule flags suspiciously-sized instances **without** evidence of capacity planning (no comment with sizing rationale, no autoscaling group, no `lifecycle.ignore_changes` linked to a recommender tool).

_Noncompliant Code Example_

```hcl
resource "aws_instance" "web" {
  instance_type = "m6i.2xlarge"   # 8 vCPU / 32 GiB — no justification, no ASG
}
```

_Compliant Solution_

```hcl
# Capacity planned via Compute Optimizer report 2026-04 (see ADR-12)
resource "aws_instance" "web" {
  instance_type = "m7g.large"    # 2 vCPU / 8 GiB
}
```

### **Rule short description**

Instances of size `>= 2xlarge` (or `>= n*-standard-8`) outside of an ASG/scaler should carry a sizing justification comment or annotation.

### **Rule justification**

**Why it matters**:
- Over-sized instances pay for unused CPU/RAM 24/7.
- Studies (Datadog, AWS Compute Optimizer) consistently report **30–60 %** of cloud spend goes to over-allocated VMs.

**Eco-design rationale** (axes 3·4·5):
- **Runtime energy**: rightsized VMs run closer to optimal CPU efficiency curve (typical CPU runs at best W/op around 50–70 % utilisation; idle is the worst).
- **Memory/CPU efficiency**: ASGs of smaller instances scale more granularly than 1 huge VM.

Sources:
- https://docs.aws.amazon.com/compute-optimizer/
- https://cloud.google.com/recommender
- https://aws.amazon.com/architecture/well-architected/sustainability/

Measurement:

| Indicator | Method | Order of magnitude |
| --- | --- | --- |
| Cloud spend | provider billing | -20 % to -40 % |
| Avg CPU utilisation | CloudWatch / Stackdriver | ↑ from ~10 % to ~50 % |

### **Severity / Remediation Cost**

Severity: **Info** — requires real usage data to confirm.

Remediation cost: **Hard** — capacity planning exercise.

### **Implementation principle**

- Target tech: Terraform HCL.
- Detection mechanism: sonar-iac Terraform visitor + heuristic.
- Algorithm:

  ```
  for each compute resource:
      size = parseSize(instance_type)
      if size >= 2xlarge_equivalent:
          if no sizing comment within 5 lines above and not part of an ASG and no `# eco-design:reviewed` annotation:
              report(resource, "Large instance without sizing rationale")
  ```

- Known false positives: HPC / GPU workloads — opt-out via tag.
- Feasibility verdict: ⚠️ heuristic (precision dependent on coding style).
- Alternative tooling: AWS Compute Optimizer, GCP Recommender, Cast.ai.
