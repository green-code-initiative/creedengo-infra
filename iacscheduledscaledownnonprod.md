_(Proposed rule for creedengo-infra — GCI1064.)_

### **Rule title**

Schedule non-prod environments to scale to zero off-hours

### **Rule key**

GCI1064

### **Language and platform**

Kubernetes (kube-green / KEDA / Knative) / Terraform schedulers / Azure Automation / GCP Cloud Scheduler

### **Rule description**

Most non-prod environments are used Monday-Friday, 09:00-19:00 — **~12 %** of the week. Running them 24/7 is a textbook eco-design anti-pattern. This rule (a transverse, environment-level counterpart of `GCI1055`) flags non-prod namespaces / projects / accounts that do not have a scheduled scale-down resource (`kube-green Sleep`, KEDA cron scaler, Knative scale-to-zero, AWS Instance Scheduler, Azure Automation Runbook).

_Noncompliant Code Example_

```yaml
# namespace dev / staging — many Deployments, no Sleep / KEDA cron scaler.
```

_Compliant Solution_

```yaml
apiVersion: kube-green.com/v1alpha1
kind: SleepInfo
metadata:
  name: weekday-sleep
  namespace: staging
spec:
  weekdays: "1-5"
  sleepAt: "20:00"
  wakeUpAt: "07:00"
  timeZone: "Europe/Paris"
```

### **Rule short description**

Each namespace / project / account labelled non-prod should declare a scheduled scale-down resource.

### **Rule justification**

**Why it matters**:
- Cutting non-prod compute to 12-hour weekdays = ~70 % of weekly hours saved on those environments.
- Aggregate impact on a typical org: 20–35 % of total compute, given the dev/prod ratio.

**Eco-design rationale** (axes 3·5·6):
- **Runtime energy**: direct -70 % on impacted resources.
- **Scale-to-zero**: aligns non-prod with "use as little as possible".
- **Provisioning efficiency**: Karpenter/Cluster Autoscaler reclaims nodes at night.

Sources:
- https://kube-green.dev/
- https://keda.sh/docs/2.13/scalers/cron/
- https://knative.dev/docs/serving/autoscaling/
- https://aws.amazon.com/blogs/architecture/field-notes-saving-cost-with-aws-instance-scheduler/

Measurement:

| Indicator | Method | Order of magnitude |
| --- | --- | --- |
| Non-prod node count off-hours | Cluster Autoscaler logs | -70 % |
| Non-prod compute hours | provider billing | -50 % to -75 % |

### **Severity / Remediation Cost**

Severity: **Major**.

Remediation cost: **Medium** — install kube-green / KEDA / scheduler infra.

### **Implementation principle**

- Target tech: K8s manifests + scheduler resources.
- Detection mechanism: custom Sensor.
- Algorithm:

  ```
  NONPROD = property("creedengo.infra.k8s.nonprodNamespaces", default=["dev","test","staging","preprod","qa"])
  for each Namespace ns whose name matches NONPROD or carries label environment in NONPROD:
      if no SleepInfo / CronScaler / Knative scale-to-zero / external scheduler references ns:
          report(ns, "Non-prod namespace without scheduled scale-down")
  ```

- Known false positives: 24/7 batch namespaces — opt-out via annotation.
- Feasibility verdict: ⚠️ (custom Sensor; cross-resource).
- Alternative tooling: kube-green, KEDA cron, Knative, AWS Instance Scheduler.
