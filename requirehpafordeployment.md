_(Proposed rule for creedengo-infra — GCI1044.)_

### **Rule title**

Production `Deployment`s should declare an HPA

### **Rule key**

GCI1044

### **Language and platform**

Kubernetes / Helm

### **Rule description**

Even a `Deployment` with `replicas: 2` benefits from an HPA: it can scale up under load (avoiding throttling and tail-latency-induced retries) and scale down to `minReplicas` during quiet periods. This rule encourages making HPA the default for any production workload, not just the "big" ones.

_Noncompliant Code Example_

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: api
  labels: { environment: production }
spec:
  replicas: 3
  template: {...}
```

_Compliant Solution_

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata: { name: api }
spec:
  scaleTargetRef: { kind: Deployment, name: api }
  minReplicas: 2
  maxReplicas: 8
  metrics:
    - type: Resource
      resource: { name: cpu, target: { type: Utilization, averageUtilization: 70 } }
```

### **Rule short description**

Each Deployment labelled `environment=production` (or matching a configurable selector) must be paired with an HPA.

### **Rule justification**

**Why it matters**:
- Manual scaling almost always means over-provisioned baseline.
- HPA is free and built-in.

**Eco-design rationale** (axes 3·4·5·6):
- **Runtime energy**: scaling down off-peak typically saves 20–60 % of compute.
- **Provisioning efficiency**: Karpenter can consolidate nodes only if pod counts vary.
- **Scale-to-zero**: combining HPA `minReplicas=0` (with KEDA) enables true scale-to-zero for event-driven workloads.

Sources:
- https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale/
- https://keda.sh/
- https://aws.amazon.com/architecture/well-architected/sustainability/

Measurement:

| Indicator | Method | Order of magnitude |
| --- | --- | --- |
| Average replica count | Prometheus | -20 % to -60 % off-peak |

### **Severity / Remediation Cost**

Severity: **Minor**.

Remediation cost: **Medium** — choose the right scaling metric and target utilisation.

### **Implementation principle**

- Target tech: Kubernetes YAML / Helm.
- Detection mechanism: sonar-iac Kubernetes visitor (cross-file).
- Algorithm:

  ```
  selector = property("creedengo.infra.k8s.productionLabelSelector", default="environment=production")
  for each Deployment d matching selector:
      if no HPA targets d: report(d, "Production Deployment without HPA")
  ```

- Known false positives: stateful services where HPA is contra-indicated — opt-out via annotation.
- Feasibility verdict: ✅
- Alternative tooling: kube-linter `dangling-service`.
