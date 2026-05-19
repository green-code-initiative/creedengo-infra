_(Proposed rule for creedengo-infra — 1055.)_

### **Rule title**

Avoid always-on resources for non-prod environments

### **Rule key**

1055

### **Language and platform**

Terraform (AWS, GCP, Azure)

### **Rule description**

Dev, test and staging environments routinely run 24/7 yet are used <50 hours/week (~30 % of total hours). The rule flags long-lived compute / database resources in non-prod environments that do **not** have a scheduler resource attached (e.g., `aws_autoscaling_schedule`, `aws_lambda_function` triggered by EventBridge, `null_resource` calling a kube-green script, Azure Automation Runbook, GCP Cloud Scheduler).

_Noncompliant Code Example_

```hcl
# env = "staging"
resource "aws_db_instance" "staging" {
  identifier       = "staging-db"
  instance_class   = "db.t3.large"
  engine           = "postgres"
  # Runs 24/7 — no schedule to stop at night.
}
```

_Compliant Solution_

```hcl
resource "aws_db_instance" "staging"        {/* as above */}
resource "aws_lambda_function" "stop_at_night" {
  function_name = "stop-staging-db"
  # triggered by EventBridge cron(0 20 * * MON-FRI *)
  # ...
}
```

### **Rule short description**

Compute / DB / cache resources tagged `environment in {dev, test, staging, preprod}` should reference a stop/start schedule resource.

### **Rule justification**

**Why it matters**:
- Non-prod typically uses ~30 % of weekly hours but pays for 100 %.
- Switching to a 8 h × 5 d schedule saves ~75 % of compute hours for these envs.

**Eco-design rationale** (axes 3·5·6):
- **Runtime energy**: direct -50 % to -75 % usage on impacted resources.
- **Scale-to-zero**: aligns non-prod with "scale to zero when idle" principle.
- **Provisioning efficiency**: scheduled scale-down reclaims node capacity for other tenants.

Sources:
- https://aws.amazon.com/blogs/architecture/field-notes-saving-cost-with-aws-instance-scheduler/
- https://kube-green.dev/
- https://learn.microsoft.com/en-us/azure/automation/automation-solution-vm-management

Measurement:

| Indicator | Method | Order of magnitude |
| --- | --- | --- |
| Non-prod compute hours | provider billing | -50 % to -75 % |

### **Severity / Remediation Cost**

Severity: **Major** — recurring waste with easy fix.

Remediation cost: **Medium** — set up scheduler infra once.

### **Implementation principle**

- Target tech: Terraform HCL.
- Detection mechanism: sonar-iac Terraform visitor.
- Algorithm:

  ```
  NONPROD_ENV = property("creedengo.infra.tf.nonprodEnvironments", default=["dev","test","staging","preprod","qa"])
  for each compute/DB/cache resource r:
      env = r.tags["environment"] or r.tags["Environment"]
      if env in NONPROD_ENV:
          if no scheduling resource (aws_autoscaling_schedule, aws_lambda + EventBridge cron, etc.) references r:
              report(r, "Non-prod resource without start/stop schedule")
  ```

- Known false positives: 24/7 nightly batch envs — opt-out via `# eco-design:always-on=batch`.
- Feasibility verdict: ⚠️ heuristic (cross-resource detection).
- Alternative tooling: AWS Instance Scheduler; kube-green for K8s namespaces.
