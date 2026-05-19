_(Proposed rule for creedengo-infra — GCI1050.)_

### **Rule title**

Prefer ARM / Graviton / Ampere instance types when available

### **Rule key**

GCI1050

### **Language and platform**

Terraform (AWS, GCP, Azure)

### **Rule description**

ARM-based instance families (AWS Graviton, GCP Tau T2A, Azure Cobalt 100/Ampere Altra) deliver **20–40 % better performance per watt** than equivalent x86_64 families at a similar or lower price point. For any workload that runs on a container image with a multi-arch build (most JVM, Node, Go, Python, Rust applications), choosing the ARM family is a one-line eco-design win.

_Noncompliant Code Example_

```hcl
resource "aws_instance" "web" {
  instance_type = "m6i.large"      # Intel Ice Lake (x86_64)
  ami           = data.aws_ami.amzn2_x86.id
}
```

_Compliant Solution_

```hcl
resource "aws_instance" "web" {
  instance_type = "m7g.large"      # Graviton 3 (ARM64)
  ami           = data.aws_ami.amzn2_arm64.id
}
```

### **Rule short description**

When the workload supports `linux/arm64`, prefer an ARM-equivalent instance family.

### **Rule justification**

**Why it matters**:
- AWS reports Graviton 3 instances offer up to 60 % better energy efficiency than comparable x86.
- Same applies to GCP Tau T2A and Azure Cobalt 100.

**Eco-design rationale** (axes 3·5):
- **Runtime energy**: -20 % to -40 % Wh for the same throughput (vendor-claimed).
- **Provisioning efficiency**: lower hourly cost (typically -10 % to -20 %) often correlates with smaller carbon footprint.

Sources:
- https://aws.amazon.com/ec2/graviton/
- https://cloud.google.com/blog/products/compute/tau-t2a-vms-with-ampere-altra-now-ga
- https://azure.microsoft.com/en-us/blog/azure-cobalt-100-based-virtual-machines-now-available/
- https://aws.amazon.com/architecture/well-architected/sustainability/

Measurement:

| Indicator | Method | Order of magnitude |
| --- | --- | --- |
| CPU·s for same workload | benchmark | -20 % to -40 % |
| Cost per hour | AWS/GCP/Azure pricing | -10 % to -20 % |

### **Severity / Remediation Cost**

Severity: **Minor**.

Remediation cost: **Medium** — requires multi-arch container builds and verification of dependencies.

### **Implementation principle**

- Target tech: Terraform HCL (AWS / GCP / Azure provider blocks).
- Detection mechanism: sonar-iac Terraform visitor.
- Algorithm:

  ```
  for each resource (aws_instance, aws_launch_template, google_compute_instance, azurerm_linux_virtual_machine):
      family = parseFamily(instance_type)
      if family in X86_FAMILIES and existsArmEquivalent(family):
          report(resource, "Prefer ARM/Graviton equivalent (" + suggestArm(family) + ")")
  X86_FAMILIES: m5*, m6i*, m7i*, c5*, c6i*, c7i*, r5*, r6i*, r7i*, t3*, ...
  ```

- Known false positives: workloads with x86-only native deps — opt-out via tag `eco-design:arch-locked`.
- Feasibility verdict: ✅
- Alternative tooling: Steampipe AWS audit; tfsec custom rule.
