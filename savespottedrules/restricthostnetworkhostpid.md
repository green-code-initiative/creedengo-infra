_(Proposed rule for creedengo-infra — 1047.)_

### **Rule title**

Restrict `hostNetwork`, `hostPID`, `hostIPC`

### **Rule key**

1047

### **Language and platform**

Kubernetes / Helm

### **Rule description**

Pods that share the host network/PID/IPC namespaces cannot be bin-packed with other pods on the same node; they often force the scheduler to dedicate a node per pod. They also bypass network policies, increasing observability cost. Only system add-ons (CNI plugins, node-local agents, eBPF probes) have a legitimate need.

_Noncompliant Code Example_

```yaml
apiVersion: apps/v1
kind: Deployment
spec:
  template:
    spec:
      hostNetwork: true
      hostPID: true
      containers: [...]
```

_Compliant Solution_

```yaml
apiVersion: apps/v1
kind: Deployment
spec:
  template:
    spec:
      # hostNetwork / hostPID / hostIPC removed for ordinary app workloads
      containers: [...]
```

### **Rule short description**

`Deployment`/`StatefulSet` should not set `hostNetwork`, `hostPID` or `hostIPC` to `true`.

### **Rule justification**

**Why it matters**:
- Host-namespace pods break scheduler assumptions (port conflicts, packing).
- They expand the security blast radius, often forcing dedicated nodes.

**Eco-design rationale** (axes 4·5):
- **Memory/CPU efficiency**: removing host-namespace usage lets the scheduler co-locate workloads, improving node density by 10–30 %.
- **Provisioning efficiency**: Karpenter can consolidate hosts when host-namespaces are not pinning placement.

Sources:
- https://kubernetes.io/docs/concepts/security/pod-security-standards/
- https://kube-linter.io/docs/checks/#host-network

Measurement:

| Indicator | Method | Order of magnitude |
| --- | --- | --- |
| Node packing | `kube-state-metrics` | +10 to +30 pp |

### **Severity / Remediation Cost**

Severity: **Info** (already covered by some security profiles, here flagged for eco-design impact).

Remediation cost: **Easy** — remove the boolean.

### **Implementation principle**

- Target tech: Kubernetes YAML / Helm.
- Detection mechanism: sonar-iac Kubernetes visitor.
- Algorithm:

  ```
  for each workload w (Deployment, StatefulSet, DaemonSet):
      ps = w.spec.template.spec
      for f in {hostNetwork, hostPID, hostIPC}:
          if ps[f] == true and not isSystemAddon(w):
              report(w, "Avoid " + f + " for application workloads")
  isSystemAddon checks namespace in {kube-system, monitoring, ingress-nginx, cilium, ...}
  ```

- Known false positives: legitimate system DaemonSets — exempt via namespace allowlist.
- Feasibility verdict: ✅
- Alternative tooling: kube-linter `host-pid`, `host-network`, `host-ipc`; PSA `Baseline` profile.
