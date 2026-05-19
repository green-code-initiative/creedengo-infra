_(Proposed rule for creedengo-infra — GCI1040.)_

### **Rule title**

Set CPU and memory `requests` on every container

### **Rule key**

GCI1040

### **Language and platform**

Kubernetes / Helm

### **Rule description**

`resources.requests` are the values the Kubernetes scheduler uses to bin-pack pods onto nodes. A container with no `requests` is treated as **zero-cost** by the scheduler: nodes get over-committed, hot spots starve real workloads, and the autoscaler reacts late. Worse, a pod without requests sits in the `BestEffort` QoS class and is the first to be evicted under pressure — triggering replacement pods, image re-pulls and additional energy use.

_Noncompliant Code Example_

```yaml
apiVersion: apps/v1
kind: Deployment
spec:
  template:
    spec:
      containers:
        - name: api
          image: myorg/api@sha256:...
```

_Compliant Solution_

```yaml
apiVersion: apps/v1
kind: Deployment
spec:
  template:
    spec:
      containers:
        - name: api
          image: myorg/api@sha256:...
          resources:
            requests:
              cpu: "100m"
              memory: "128Mi"
```

### **Rule short description**

Every container in `Deployment`, `StatefulSet`, `DaemonSet`, `Job`, `CronJob` must declare `resources.requests.cpu` and `resources.requests.memory`.

### **Rule justification**

**Why it matters**:
- Without requests, the scheduler cannot place pods deterministically → over-provisioned nodes "look full" but run idle (Guaranteed) workloads alongside busy (BestEffort) ones.
- Cluster Autoscaler / Karpenter add nodes only when requests exceed capacity; missing requests block both scale-up and scale-down decisions.

**Eco-design rationale** (axes 3·4·5·6):
- **Runtime energy**: accurate requests enable bin-packing → typically -10 % to -30 % nodes for the same workload (KRR, Goldilocks studies).
- **Memory/CPU efficiency**: scheduler can co-locate workloads with complementary peak hours.
- **Provisioning efficiency**: Karpenter / Cluster Autoscaler can finally consolidate underused nodes and scale down.
- **Scale-to-zero**: KEDA/Knative metrics rely on requests for ratio-based scaling decisions.

Sources:
- https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/
- https://learnk8s.io/setting-cpu-memory-limits-requests
- https://aws.amazon.com/architecture/well-architected/sustainability/

Measurement:

| Indicator | Method | Order of magnitude |
| --- | --- | --- |
| Node count | Cluster Autoscaler logs | -10 % to -30 % |
| CPU/RAM idle ratio | `kube-state-metrics` | -X percentage points |

### **Severity / Remediation Cost**

Severity: **Major** — direct, large-scale effect on scheduling efficiency.

Remediation cost: **Easy** — add 4 YAML lines per container.

### **Implementation principle**

- Target tech: Kubernetes YAML / Helm templates.
- Detection mechanism: sonar-iac Kubernetes visitor (`org.sonar.iac.kubernetes.*` — TBD).
- Algorithm:

  ```
  for each workload (Deployment, StatefulSet, DaemonSet, Job, CronJob):
      for each container in spec.template.spec.containers + initContainers:
          if container.resources.requests.cpu missing: report(container, "Set requests.cpu")
          if container.resources.requests.memory missing: report(container, "Set requests.memory")
  ```

- Known false positives: experimental dev manifests — opt-out via namespace label `eco-design=ignore`.
- Feasibility verdict: ✅
- Alternative tooling: kube-linter `no-resource-requests`; KRR for actual rightsizing values.
