_(Proposed rule for creedengo-infra — 1043.)_

### **Rule title**

Avoid hard-coded high replica counts without HPA

### **Rule key**

1043

### **Language and platform**

Kubernetes / Helm

### **Rule description**

A `Deployment` with `replicas: 10` and no `HorizontalPodAutoscaler` runs 10 pods 24/7, regardless of load. This is the canonical eco-design anti-pattern: paying for the peak even at night/weekends. Either lower the static replicas and add an HPA, or justify the value (e.g., quorum-based services like ZooKeeper/etcd).

_Noncompliant Code Example_

```yaml
apiVersion: apps/v1
kind: Deployment
metadata: { name: web }
spec:
  replicas: 10
  template: {...}
# No HPA defined anywhere.
```

_Compliant Solution_

```yaml
apiVersion: apps/v1
kind: Deployment
metadata: { name: web }
spec:
  replicas: 2
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata: { name: web }
spec:
  scaleTargetRef: { kind: Deployment, name: web }
  minReplicas: 2
  maxReplicas: 10
  metrics:
    - type: Resource
      resource: { name: cpu, target: { type: Utilization, averageUtilization: 70 } }
```

### **Rule short description**

`Deployment.spec.replicas > THRESHOLD` (default 3) must be paired with an HPA targeting the same workload.

### **Rule justification**

**Why it matters**:
- Static high replicas waste resources during off-peak hours (often 60–80 % of the week).
- HPA can also scale down to `minReplicas`, matching demand at all times.

**Eco-design rationale** (axes 3·4·5·6):
- **Runtime energy**: replacing 10 static pods by HPA `min=2, max=10` typically saves 40–70 % of compute over a week (estimated, workload-dependent).
- **Memory/CPU efficiency**: cluster reclaims unused capacity for other workloads at night.
- **Scale-to-zero / scale-to-fit**: opens the door to KEDA event-driven scaling or kube-green time-based scaling.
- **Provisioning efficiency**: Karpenter consolidates nodes when replicas drop.

Sources:
- https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale/
- https://keda.sh/
- https://kube-green.dev/

Measurement:

| Indicator | Method | Order of magnitude |
| --- | --- | --- |
| Average pod count | Prometheus `kube_deployment_status_replicas` | -40 % to -70 % |
| Node count off-peak | Cluster Autoscaler logs | -X nodes |

### **Severity / Remediation Cost**

Severity: **Minor** — requires capacity-planning judgement.

Remediation cost: **Hard** — install HPA, set proper metric & utilisation target, possibly install metrics-server / KEDA.

### **Implementation principle**

- Target tech: Kubernetes YAML / Helm.
- Detection mechanism: sonar-iac Kubernetes visitor (cross-file).
- Algorithm:

  ```
  THRESHOLD = property("creedengo.infra.k8s.staticReplicasThreshold", default=3)
  for each Deployment d in project:
      if d.spec.replicas > THRESHOLD:
          if no HPA targets d by (apiVersion, kind, name):
              report(d, "Static replicas > " + THRESHOLD + " without HPA")
  ```

- Known false positives: quorum systems (etcd, ZooKeeper, Kafka) — opt-out via annotation `eco-design.creedengo.io/quorum=true`.
- Feasibility verdict: ⚠️ heuristic (cross-file resolution required).
- Alternative tooling: kube-linter `dangling-hpa-target`; KRR.
