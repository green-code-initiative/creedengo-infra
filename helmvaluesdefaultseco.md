_(Proposed rule for creedengo-infra — 1049.)_

### **Rule title**

Eco-design defaults in Helm `values.yaml`

### **Rule key**

1049

### **Language and platform**

Helm

### **Rule description**

Helm charts are deployed thousands of times: their **default** `values.yaml` shapes the carbon footprint of every consumer. A chart whose defaults declare `replicaCount: 3`, no HPA, no resource requests/limits, `imagePullPolicy: Always` and oversize JVM heap propagates wasteful patterns. Eco-design demands that defaults be the **minimum reasonable** configuration, with opt-in for scaling.

_Noncompliant Code Example_

```yaml
# values.yaml
replicaCount: 3
image:
  pullPolicy: Always
resources: {}     # no requests/limits
autoscaling:
  enabled: false
```

_Compliant Solution_

```yaml
# values.yaml
replicaCount: 1
image:
  pullPolicy: IfNotPresent
resources:
  requests: { cpu: "50m",  memory: "64Mi" }
  limits:   {              memory: "128Mi" }
autoscaling:
  enabled: true
  minReplicas: 1
  maxReplicas: 4
  targetCPUUtilizationPercentage: 70
```

### **Rule short description**

`values.yaml` defaults must declare resource requests, sensible limits, `IfNotPresent` pull policy, and enable autoscaling.

### **Rule justification**

**Why it matters**:
- Helm chart defaults are propagated across every adoption.
- A chart pulled 10 000 times that defaults to 3 replicas instead of 1 produces 20 000 unnecessary pods.

**Eco-design rationale** (axes 3·4·5·6):
- **Runtime energy**: fleet-wide effect; even modest savings per install multiply by the install count.
- **Memory/CPU efficiency**: smart defaults teach downstream operators good habits.
- **Scale-to-fit**: autoscaling enabled by default makes scale-to-fit the norm, not the exception.

Sources:
- https://helm.sh/docs/chart_best_practices/values/
- https://aws.amazon.com/architecture/well-architected/sustainability/
- https://greensoftware.foundation/articles/sci-specification

Measurement:

| Indicator | Method | Order of magnitude |
| --- | --- | --- |
| Default pod count | `helm template` + count | -50 % to -67 % |
| Average resource reservation | aggregated across users | -X CPU·core, -X GB RAM |

### **Severity / Remediation Cost**

Severity: **Info** — applies to chart authors.

Remediation cost: **Medium** — adjust defaults, document opt-in scaling.

### **Implementation principle**

- Target tech: Helm `values.yaml`.
- Detection mechanism: custom Sensor analysing Helm chart roots.
- Algorithm:

  ```
  for each chart root (Chart.yaml present):
      v = parse values.yaml
      checks:
        v.replicaCount > 1 without v.autoscaling.enabled → report
        v.resources empty → report
        v.image.pullPolicy == "Always" → report
        v.autoscaling.enabled == false → report (Info)
  ```

- Known false positives: charts for quorum systems / databases — opt-out via `Chart.yaml` annotation `creedengo.io/eco-design: quorum`.
- Feasibility verdict: ⚠️ heuristic (custom Sensor; YAML is parsed but semantics are chart-specific).
- Alternative tooling: `helm template | kube-linter` pipeline.
