_(Proposed rule for creedengo-infra — 1034.)_

### **Rule title**

Expose only the ports actually used at runtime

### **Rule key**

1034

### **Language and platform**

Docker / OCI

### **Rule description**

`EXPOSE` is purely informational, but downstream orchestrators (Compose, K8s manifests, service meshes, ingress controllers) use it as a hint to open network paths, declare service ports and configure load balancers. Declaring many `EXPOSE` ports — or default debug/admin ports never reached in production — wastes service-mesh CPU, sidecar memory and observability ingestion quota.

_Noncompliant Code Example_

```Dockerfile
FROM nginx:1.27-alpine
EXPOSE 80 443 8080 9090 9091 9100 5000
```

_Compliant Solution_

```Dockerfile
FROM nginx:1.27-alpine
# Only the public HTTPS port is needed at runtime
EXPOSE 443
```

### **Rule short description**

Declare a single `EXPOSE` instruction listing only the ports the application actually serves in production.

### **Rule justification**

**Why it matters**:
- Excess `EXPOSE` declarations propagate into Compose/K8s/Service definitions and waste sidecar/ingress resources.
- Each open path increases attack surface and observability noise.

**Eco-design rationale** (axes 3·4·5):
- **Runtime energy**: each port mapping in a service mesh (Istio/Linkerd) costs sidecar CPU (~5–20 mCPU per port instrumented). Over a 500-container fleet, dropping 5 unused ports saves several CPU·core·hours/day.
- **Memory efficiency**: ingress/load-balancer rule sets grow linearly with declared ports.
- **Provisioning efficiency**: fewer K8s probes/health checks attached to non-existent endpoints.

Sources:
- https://docs.docker.com/engine/reference/builder/#expose
- https://kubernetes.io/docs/concepts/services-networking/service/
- https://github.com/hadolint/hadolint/wiki/DL3011

Measurement:

| Indicator | Method | Order of magnitude |
| --- | --- | --- |
| Sidecar CPU | `kubectl top pod istio-proxy` | -5 to -20 mCPU per dropped port |
| Mesh control-plane memory | Pilot/istiod metrics | linear with declared ports |

### **Severity / Remediation Cost**

Severity: **Minor** — modest per-container impact, large fleet-wide effect.

Remediation cost: **Easy** — keep only the public-facing port.

### **Implementation principle**

- Target tech: Docker / OCI Dockerfile.
- Detection mechanism: sonar-iac Docker visitor.
- Algorithm:

  ```
  expose = collect all EXPOSE instructions in file
  ports = flatten(expose[i].ports())
  if size(ports) > THRESHOLD (default 2):
      reportIssue(expose[0], "Declare only the ports actually served at runtime")
  if any port in DEFAULT_ADMIN_PORTS and no comment justifies it:
      reportIssue(expose[i], "Avoid exposing admin/debug port " + port)
  DEFAULT_ADMIN_PORTS: 8080-aux, 9090, 9091, 9100, 5005 (jdwp), 5000 (flask debug), 3000-internal
  ```

- Known false positives: gateways / proxies that legitimately expose many ports — allow via property `creedengo.infra.expose.allowlist`.
- Feasibility verdict: ✅
- Alternative tooling: Hadolint **DL3011** (only validates the range).
