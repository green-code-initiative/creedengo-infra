_(Proposed rule for creedengo-infra — GCI1045.)_

### **Rule title**

Pair workloads with a rightsizing recommender (VPA / KRR / Goldilocks)

### **Rule key**

GCI1045

### **Language and platform**

Kubernetes / Helm

### **Rule description**

Static `requests`/`limits` drift over time as code evolves. A rightsizing recommender (`VerticalPodAutoscaler` in `Off` or `Recommendation` mode, `Robusta KRR`, `Fairwinds Goldilocks`) continuously analyses real usage and surfaces the values that match actual demand — usually revealing 30–80 % over-allocation.

_Noncompliant Code Example_

```yaml
apiVersion: apps/v1
kind: Deployment
metadata: { name: api }
spec:
  template:
    spec:
      containers:
        - name: api
          resources:
            requests: { cpu: "1000m", memory: "2Gi" }
# No VPA / KRR report referenced; values were chosen "to be safe" 18 months ago.
```

_Compliant Solution_

```yaml
apiVersion: autoscaling.k8s.io/v1
kind: VerticalPodAutoscaler
metadata: { name: api }
spec:
  targetRef: { apiVersion: apps/v1, kind: Deployment, name: api }
  updatePolicy: { updateMode: "Off" }   # recommendation-only
```

### **Rule short description**

Production workloads should either be covered by a VPA (in `Off`/`Initial`/`Recreate` mode) or be reviewed by a periodic rightsizing tool (KRR/Goldilocks) — record this in the manifest via a Helm value or annotation.

### **Rule justification**

**Why it matters**:
- Real production usage is typically 20–40 % of reserved capacity (Datadog State of Cloud Costs 2024).
- VPA/KRR turns over-allocation into measurable, fixable issues.

**Eco-design rationale** (axes 3·4·5·6):
- **Runtime energy**: rightsizing studies routinely report -30 % to -60 % cluster CPU.
- **Memory efficiency**: same workload on fewer nodes → better bin-packing.
- **Scale-to-fit**: closes the loop between actual demand and reserved capacity.
- **Provisioning efficiency**: Karpenter can shrink the node pool as requests drop.

Sources:
- https://github.com/kubernetes/autoscaler/tree/master/vertical-pod-autoscaler
- https://github.com/robusta-dev/krr
- https://github.com/FairwindsOps/goldilocks
- https://aws.amazon.com/architecture/well-architected/sustainability/

Measurement:

| Indicator | Method | Order of magnitude |
| --- | --- | --- |
| Cluster CPU | Prometheus | -30 % to -60 % |
| Node count | Cluster Autoscaler logs | -X nodes |

### **Severity / Remediation Cost**

Severity: **Info** — organisational/process recommendation.

Remediation cost: **Hard** — requires installing VPA/KRR/Goldilocks, defining a review cadence.

### **Implementation principle**

- Target tech: Kubernetes manifests + organisational process.
- Detection mechanism: ❌ not implementable statically; relies on cluster-runtime data.
- Algorithm: surface external KRR/Goldilocks reports via `sonar.externalIssuesReportPaths` (JSON converter under `creedengo-infra/tools/import-krr/`).
- Feasibility verdict: ❌ (delegate to KRR/Goldilocks/VPA).
- Alternative tooling: KRR, Goldilocks, VPA recommender, Datadog/New Relic rightsizing widgets.
