_(Proposed rule for creedengo-infra — 1046.)_

### **Rule title**

Avoid `imagePullPolicy: Always` on stable tags / digests

### **Rule key**

1046

### **Language and platform**

Kubernetes / Helm

### **Rule description**

`imagePullPolicy: Always` re-checks the registry on every pod start, even for immutable digest references. The check itself costs round-trips, registry-API CPU and may trigger a re-pull when transient registry behaviour reports a "different" digest. Prefer `IfNotPresent` (the default for non-`:latest` tags) and pin images with digests (`1030`).

_Noncompliant Code Example_

```yaml
containers:
  - name: api
    image: myorg/api@sha256:b5d2...
    imagePullPolicy: Always
```

_Compliant Solution_

```yaml
containers:
  - name: api
    image: myorg/api@sha256:b5d2...
    imagePullPolicy: IfNotPresent
```

### **Rule short description**

`imagePullPolicy` must be `IfNotPresent` (or omitted) when the image reference is pinned by digest or by an explicit version tag.

### **Rule justification**

**Why it matters**:
- Pinned-by-digest images can never change → `Always` triggers a useless registry round-trip on every restart.
- On clusters with hundreds of restarts per hour, this is non-trivial registry load.

**Eco-design rationale** (axes 3·5·7):
- **Runtime energy**: removes thousands of registry HEAD calls per day on a busy cluster.
- **Provisioning efficiency**: pod startup latency improves (no manifest fetch round-trip).
- **Storage footprint**: registry billed per request on some providers (ECR/GCR egress).

Sources:
- https://kubernetes.io/docs/concepts/containers/images/#image-pull-policy
- https://docs.docker.com/engine/reference/commandline/pull/

Measurement:

| Indicator | Method | Order of magnitude |
| --- | --- | --- |
| Registry HEAD calls | Registry metrics | -N k/day on a 500-pod cluster |
| Pod startup latency | `kubectl describe pod` "Pulled" timing | -100 to -500 ms |

### **Severity / Remediation Cost**

Severity: **Minor**.

Remediation cost: **Easy** — change one YAML value.

### **Implementation principle**

- Target tech: Kubernetes YAML / Helm.
- Detection mechanism: sonar-iac Kubernetes visitor.
- Algorithm:

  ```
  for each container:
      if container.imagePullPolicy == "Always":
          if container.image contains "@sha256:" or has explicit non-:latest tag:
              report(container, "Use IfNotPresent on pinned/tagged images")
  ```

- Known false positives: development namespaces that intentionally roll a moving tag — opt-out via namespace label.
- Feasibility verdict: ✅
- Alternative tooling: kube-linter `latest-tag`, OPA Gatekeeper templates.
