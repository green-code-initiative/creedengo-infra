_(Proposed rule for creedengo-infra — GCI1056.)_

### **Rule title**

Prefer Spot / Preemptible / Savings Plan capacity for fault-tolerant workloads

### **Rule key**

GCI1056

### **Language and platform**

Terraform (AWS, GCP, Azure)

### **Rule description**

Spot / Preemptible / Low-Priority VMs use **otherwise-idle cloud capacity** (50–90 % cheaper, and consuming what would have run unused). For fault-tolerant workloads (batch, CI runners, stateless web tiers behind LB), this is a textbook eco-design win: same job, fewer marginal kWh, same emissions accounted for over more useful work.

_Noncompliant Code Example_

```hcl
resource "aws_autoscaling_group" "workers" {
  launch_template { id = aws_launch_template.workers.id }
  # 100 % On-Demand — no spot capacity
}
```

_Compliant Solution_

```hcl
resource "aws_autoscaling_group" "workers" {
  mixed_instances_policy {
    instances_distribution {
      on_demand_base_capacity                  = 1
      on_demand_percentage_above_base_capacity = 20
      spot_allocation_strategy                 = "price-capacity-optimized"
    }
    launch_template { launch_template_specification { launch_template_id = aws_launch_template.workers.id } }
  }
}
```

### **Rule short description**

ASG / MIG / VMSS for stateless workloads should mix Spot/Preemptible capacity.

### **Rule justification**

**Why it matters**:
- Spot uses unused datacenter capacity → marginal carbon ≈ 0 (capacity is already there).
- 50–90 % cheaper than On-Demand.

**Eco-design rationale** (axes 3·6):
- **Runtime energy**: spot work amortises the datacenter's "always on" footprint over more useful work.
- **Provisioning efficiency**: encourages fault-tolerant architecture (retries, idempotency).

Sources:
- https://aws.amazon.com/ec2/spot/
- https://cloud.google.com/compute/docs/instances/preemptible
- https://learn.microsoft.com/en-us/azure/virtual-machines/spot-vms

Measurement:

| Indicator | Method | Order of magnitude |
| --- | --- | --- |
| Cost | provider billing | -50 % to -90 % |

### **Severity / Remediation Cost**

Severity: **Info** — requires fault-tolerant workload design.

Remediation cost: **Hard** — implement retries, checkpointing, handle interruption signals.

### **Implementation principle**

- Target tech: Terraform HCL.
- Detection mechanism: sonar-iac Terraform visitor.
- Algorithm:

  ```
  for each aws_autoscaling_group / google_compute_instance_group_manager / azurerm_virtual_machine_scale_set asg:
      if labels/tags indicate stateless workload (web, worker, runner, batch):
          if asg has no spot/preemptible capacity: report(asg, "Consider mixing Spot/Preemptible capacity")
  ```

- Known false positives: stateful or latency-critical fleets — opt-out via tag `workload=stateful`.
- Feasibility verdict: ⚠️ heuristic.
- Alternative tooling: Karpenter `consolidationPolicy: WhenUnderutilized` + spot pools.
