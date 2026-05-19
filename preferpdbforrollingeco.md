_(Proposed rule for creedengo-infra — GCI1048.)_

### **Rule title**

Define a `PodDisruptionBudget` for multi-replica workloads

### **Rule key**

GCI1048

### **Language and platform**

Kubernetes / Helm

### **Rule description**

A `PodDisruptionBudget` (PDB) tells the cluster how many replicas must remain available during voluntary disruptions (node drains, autoscaler consolidation, upgrades). Without a PDB, Karpenter/Cluster Autoscaler must conservatively avoid evictions, leaving nodes underused. With a tight PDB (`minAvailable: 50%`), the autoscaler can consolidate aggressively while protecting service availability.

_Noncompliant Code Example_

```yaml
apiVersion: apps/v1
kind: Deployment
spec:
  replicas: 4
# No PodDisruptionBudget defined.
```

_Compliant Solution_

```yaml
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata: { name: api }
spec:
  minAvailable: 50%
  selector:
    matchLabels: { app: api }
```

### **Rule short description**

`Deployment`/`StatefulSet` with `replicas >= 2` should have a matching `PodDisruptionBudget`.

### **Rule justification**

**Why it matters**:
- Without PDB, the autoscaler is too cautious → nodes stay up while half-empty.
- PDB also protects from accidental simultaneous disruption.

**Eco-design rationale** (axes 4·5·6):
- **Memory/CPU efficiency**: aggressive node consolidation by Karpenter / Cluster Autoscaler.
- **Provisioning efficiency**: faster, predictable rolling updates.
- **Scale-to-fit**: the cluster can shrink to match real demand without violating availability SLOs.

Sources:
- https://kubernetes.io/docs/concepts/workloads/pods/disruptions/
- https://karpenter.sh/docs/concepts/disruption/

Measurement:

| Indicator | Method | Order of magnitude |
| --- | --- | --- |
| Node consolidation events | Karpenter metrics | +X /day after PDB |

### **Severity / Remediation Cost**

Severity: **Info** — primarily affects autoscaler efficiency.

Remediation cost: **Medium** — choose appropriate `minAvailable` / `maxUnavailable` per service SLO.

### **Implementation principle**

- Target tech: Kubernetes YAML / Helm.
- Detection mechanism: sonar-iac Kubernetes visitor (cross-file).
- Algorithm:

  ```
  for each Deployment/StatefulSet w with replicas >= 2:
      if no PDB selector matches w.spec.selector: report(w, "Missing PodDisruptionBudget")
  ```

- Known false positives: workloads using `topologySpreadConstraints` only — still report (PDB remains best practice).
- Feasibility verdict: ✅
- Alternative tooling: kube-linter `pdb-min-available`; Karpenter built-in checks.
