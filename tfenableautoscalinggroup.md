_(Proposed rule for creedengo-infra — 1052.)_

### **Rule title**

Compute resources should belong to an autoscaling group / managed instance group

### **Rule key**

1052

### **Language and platform**

Terraform (AWS, GCP, Azure)

### **Rule description**

A bare `aws_instance` / `google_compute_instance` / `azurerm_linux_virtual_machine` runs 24/7 with no scale-out and no scale-in. Wrap stateless compute in an autoscaling group (`aws_autoscaling_group`, `google_compute_instance_group_manager`, `azurerm_virtual_machine_scale_set`) so capacity tracks demand and pays only for what is used.

_Noncompliant Code Example_

```hcl
resource "aws_instance" "worker" {
  count         = 4
  instance_type = "m7g.large"
}
```

_Compliant Solution_

```hcl
resource "aws_launch_template" "worker" {
  instance_type = "m7g.large"
}
resource "aws_autoscaling_group" "worker" {
  min_size         = 1
  max_size         = 10
  desired_capacity = 2
  launch_template { id = aws_launch_template.worker.id, version = "$Latest" }
}
```

### **Rule short description**

Stateless `aws_instance` / `google_compute_instance` / `azurerm_*virtual_machine` should be replaced by an ASG / MIG / VMSS.

### **Rule justification**

**Why it matters**:
- Static fleets always pay for peak capacity, even at 3 a.m.
- ASG + scaling policy can typically shave 30–60 % of compute spend on bursty workloads.

**Eco-design rationale** (axes 3·5·6):
- **Runtime energy**: capacity tracks demand → fewer idle CPU·s.
- **Scale-to-fit**: combined with predictive scaling, capacity follows time-of-day patterns.
- **Provisioning efficiency**: ASGs can integrate with spot/preemptible markets (`1056`).

Sources:
- https://docs.aws.amazon.com/autoscaling/
- https://cloud.google.com/compute/docs/instance-groups
- https://learn.microsoft.com/en-us/azure/virtual-machine-scale-sets/

Measurement:

| Indicator | Method | Order of magnitude |
| --- | --- | --- |
| Compute spend | provider billing | -30 % to -60 % |

### **Severity / Remediation Cost**

Severity: **Major**.

Remediation cost: **Medium** — refactor `count` blocks into ASG + launch template.

### **Implementation principle**

- Target tech: Terraform HCL.
- Detection mechanism: sonar-iac Terraform visitor.
- Algorithm:

  ```
  for each aws_instance/google_compute_instance/azurerm_linux_virtual_machine r:
      if r.count > 1 and no surrounding ASG/MIG/VMSS module:
          report(r, "Use an autoscaling group / MIG / VMSS instead of count")
  ```

- Known false positives: bastion hosts, jump boxes (count=1) — exclude when count == 1.
- Feasibility verdict: ✅
- Alternative tooling: tfsec custom rule; Checkov `CKV_AWS_*`.
