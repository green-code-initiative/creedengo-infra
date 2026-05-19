_(Proposed rule for creedengo-infra — 1041.)_

### **Rule title**

Set memory `limits` on every container (and a CPU limit when appropriate)

### **Rule key**

1041

### **Language and platform**

Kubernetes / Helm

### **Rule description**

A container without a memory `limit` can grow until it exhausts the node and triggers an OOMKill cascade affecting unrelated pods. Each cascade costs energy (re-schedule, re-pull, re-start). A memory limit is mandatory for predictable bin-packing. A CPU limit is more nuanced (it can throttle benign bursts), but it must be set for batch/CronJob workloads to prevent runaway loops monopolising a node.

_Noncompliant Code Example_

```yaml
resources:
  requests:
    cpu: "100m"
    memory: "128Mi"
```

_Compliant Solution_

```yaml
resources:
  requests:
    cpu: "100m"
    memory: "128Mi"
  limits:
    memory: "256Mi"    # mandatory for predictable QoS
    # cpu: "500m"      # set for batch/CronJob workloads
```

### **Rule short description**

Every container must declare `resources.limits.memory`. Batch/Job/CronJob workloads must also declare `resources.limits.cpu`.

### **Rule justification**

**Why it matters**:
- Memory leaks without a limit take down whole nodes (OOM-Kill cascade).
- Unbounded CronJob/batch loops can starve real-time workloads.

**Eco-design rationale** (axes 3·4·5):
- **Memory efficiency**: limits enable Guaranteed QoS pods to be tightly packed; without them, the scheduler must reserve large safety margins per node.
- **Runtime energy**: fewer OOMKill→restart cycles → less re-pull, less re-init.
- **Provisioning efficiency**: Karpenter consolidation can move pods predictably because their max footprint is known.

Sources:
- https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/#requests-and-limits
- https://learnk8s.io/production-best-practices/#resource-management
- https://home.robusta.dev/blog/stop-using-cpu-limits

Measurement:

| Indicator | Method | Order of magnitude |
| --- | --- | --- |
| OOMKill events | `kubectl get events --field-selector reason=OOMKilling` | ↓ after enforcement |
| Node packing ratio | `kube-state-metrics` | +5 to +20 pp |

### **Severity / Remediation Cost**

Severity: **Major**.

Remediation cost: **Easy**.

### **Implementation principle**

- Target tech: Kubernetes YAML / Helm.
- Detection mechanism: sonar-iac Kubernetes visitor.
- Algorithm:

  ```
  for each container in workload.spec.template.spec:
      if resources.limits.memory missing: report(container, "Set limits.memory")
      if workload.kind in {Job, CronJob} and resources.limits.cpu missing:
          report(container, "Set limits.cpu for batch workloads")
  ```

- Known false positives: workloads using LimitRange / VPA at namespace level — exempt when a `LimitRange` is detected in the same namespace.
- Feasibility verdict: ✅
- Alternative tooling: kube-linter `no-memory-limits`; OPA Gatekeeper templates.
