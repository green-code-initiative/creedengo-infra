_(Proposed rule for creedengo-infra — 1063.)_

### **Rule title**

Default Prometheus / metrics retention should be short

### **Rule key**

1063

### **Language and platform**

Helm / Kubernetes (Prometheus operator) / Terraform (managed observability)

### **Rule description**

A Prometheus instance with `retention: 90d` and `scrapeInterval: 15s` ingests **hundreds of millions** of samples and consumes terabytes of disk per cluster. For most teams, raw retention beyond 30 days is wasted: long-term analysis uses downsampled remote-write (Thanos, Mimir, Cortex, GCP Managed, Amazon AMP) at a fraction of the storage cost and energy. This rule flags Prometheus deployments / managed-observability configs where retention exceeds a configurable threshold without an associated downsampling target.

_Noncompliant Code Example_

```yaml
# values.yaml for kube-prometheus-stack
prometheus:
  prometheusSpec:
    retention: 180d
    scrapeInterval: 10s
    retentionSize: 500Gi
```

_Compliant Solution_

```yaml
prometheus:
  prometheusSpec:
    retention: 15d
    scrapeInterval: 30s
    retentionSize: 50Gi
    remoteWrite:
      - url: https://thanos-receiver.example.com/api/v1/receive
```

### **Rule short description**

Raw Prometheus retention should default to ≤ 30 days; longer retention must be coupled with a remote-write to a downsampling store.

### **Rule justification**

**Why it matters**:
- A 100-node cluster with 15 s scrape and 90 d retention can consume 500 GB+ of fast SSD per Prometheus replica.
- Downsampled long-term storage (Thanos compactor 5-minute resolution) cuts that by 60–90 %.

**Eco-design rationale** (axes 4·5·7):
- **Memory/CPU efficiency**: Prometheus uses RAM proportional to active series; shorter retention reduces compaction overhead.
- **Storage footprint**: fast SSD billed per GB-month; -80 % storage on a typical observability stack.
- **Runtime energy**: continuous compaction and disk I/O scale with retention size.

Sources:
- https://prometheus.io/docs/prometheus/latest/storage/
- https://thanos.io/
- https://grafana.com/oss/mimir/

Measurement:

| Indicator | Method | Order of magnitude |
| --- | --- | --- |
| Prometheus PVC size | `kubectl get pvc` | -50 % to -90 % |
| Cluster IOPS | provider metrics | -X % |

### **Severity / Remediation Cost**

Severity: **Info**.

Remediation cost: **Medium** — set up remote-write / managed observability.

### **Implementation principle**

- Target tech: Helm values + K8s manifests.
- Detection mechanism: custom Sensor + Kubernetes/Helm visitor.
- Algorithm:

  ```
  RETENTION_MAX_DAYS = property("creedengo.infra.observability.retentionMaxDays", default=30)
  for each Prometheus CR / Helm values block declaring "retention":
      d = parseRetentionDays(value)
      if d > RETENTION_MAX_DAYS and no remoteWrite block present:
          report(...)
  ```

- Known false positives: compliance use-cases requiring long retention — opt-out via annotation.
- Feasibility verdict: ⚠️ (custom Sensor).
- Alternative tooling: kube-linter custom check; Grafana Cloud cost analyser.
