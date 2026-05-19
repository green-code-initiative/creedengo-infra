_(Proposed rule for creedengo-infra — GCI1042.)_

### **Rule title**

Keep a reasonable `request` vs `limit` ratio (1× to 4×)

### **Rule key**

GCI1042

### **Language and platform**

Kubernetes / Helm

### **Rule description**

A 1:10 ratio between request and limit (e.g., `requests.cpu=100m`, `limits.cpu=1000m`) signals chronic over-allocation: pods grab much more than they reserved, leading to noisy-neighbour contention, throttling and unpredictable scaling. A 1:1 ratio prevents any burst tolerance. The eco-design sweet spot is between **1×** (Guaranteed QoS) and **~4×** (Burstable, controlled head-room).

_Noncompliant Code Example_

```yaml
resources:
  requests: { cpu: "100m",  memory: "64Mi" }
  limits:   { cpu: "2000m", memory: "2Gi" }   # 20× ratio
```

_Compliant Solution_

```yaml
resources:
  requests: { cpu: "200m", memory: "256Mi" }
  limits:   { cpu: "500m", memory: "512Mi" }  # 2.5× ratio
```

### **Rule short description**

For each container, `limits.<resource> / requests.<resource>` should stay between 1 and 4 (configurable).

### **Rule justification**

**Why it matters**:
- A high ratio defeats scheduler placement (real usage ≫ reservation) → noisy-neighbour incidents.
- A 1:1 ratio prevents any short-lived peak smoothing and forces per-pod over-provisioning.

**Eco-design rationale** (axes 3·4·5):
- **Memory/CPU efficiency**: the right ratio lets the scheduler trust requests for bin-packing.
- **Runtime energy**: throttling events caused by high-ratio bursts force CPU stalls, increasing tail latency and triggering more replicas via HPA.
- **Provisioning efficiency**: HPA scales on `request` ratio; misaligned ratios cause flapping scale events.

Sources:
- https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/
- https://home.robusta.dev/blog/kubernetes-memory-limit
- https://krr.io/

Measurement:

| Indicator | Method | Order of magnitude |
| --- | --- | --- |
| CPU throttling | `container_cpu_cfs_throttled_seconds_total` | ↓ after tuning |
| HPA flapping | HPA event log | ↓ after ratio fix |

### **Severity / Remediation Cost**

Severity: **Minor** — heuristic; tied to workload profile.

Remediation cost: **Medium** — requires reading historical metrics (KRR / VPA recommender).

### **Implementation principle**

- Target tech: Kubernetes YAML / Helm.
- Detection mechanism: sonar-iac Kubernetes visitor.
- Algorithm:

  ```
  RATIO_MAX = property("creedengo.infra.k8s.requestLimitRatio", default=4)
  for each container:
      for r in {cpu, memory}:
          if limits[r] / requests[r] > RATIO_MAX: report(container, "Ratio " + r + " too high")
  ```

- Known false positives: workloads with intentional burst profile (job runners) — opt-out via annotation.
- Feasibility verdict: ✅
- Alternative tooling: KRR, Goldilocks, VPA recommender.
