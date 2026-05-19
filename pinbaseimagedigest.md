_(Proposed rule for creedengo-infra — GCI1030.)_

### **Rule title**

Pin Docker base images by content digest

### **Rule key**

GCI1030

### **Language and platform**

Docker / OCI

### **Rule description**

Tags are mutable: `python:3.11-slim` today is not the same image as `python:3.11-slim` next week. Each silent re-tag forces a re-pull, re-decompression, re-deploy and invalidates layer caches across every build node, CI runner and Kubernetes node. Pin base images by **content digest** (`@sha256:…`) to make builds reproducible and to stop wasting CPU·s and bandwidth on unintended re-downloads.

_Noncompliant Code Example_

```Dockerfile
FROM eclipse-temurin:17-jre-alpine
```

_Compliant Solution_

```Dockerfile
FROM eclipse-temurin:17-jre-alpine@sha256:b5d2f1f6...e3f8
```

### **Rule short description**

Each `FROM` instruction must include an `@sha256:` digest in addition to the tag.

### **Rule justification**

**Why it matters**:
- Reproducible builds: a re-run from the same commit produces the same image bytes.
- Eliminates unplanned re-pulls when an upstream maintainer republishes a tag.

**Eco-design rationale** (axes 2·3·5·7):
- **Buildtime energy**: stops "phantom rebuilds" caused by upstream re-tags; protects BuildKit cache hits.
- **Runtime energy**: K8s nodes with `imagePullPolicy: IfNotPresent` no longer detect a "new" image just because a tag moved; this avoids re-pulling 50–500 MB across every node at unpredictable times.
- **Provisioning efficiency**: deterministic rollouts; canary/blue-green deployments observe exactly what they validated.
- **Storage footprint**: registry deduplication is more effective when all clients ask for the same digest.

Sources:
- https://docs.docker.com/engine/reference/builder/#from
- https://docs.docker.com/build/cache/optimize/#use-immutable-tags
- https://snyk.io/blog/best-practices-for-container-images/

Measurement:

| Indicator | Method | Order of magnitude |
| --- | --- | --- |
| Unplanned image re-pulls | Registry pull metrics over a month | -50 % to -90 % |
| Build determinism | `docker build --no-cache` reproducibility | 100 % |

### **Severity / Remediation Cost**

Severity: **Minor** — energy cost is intermittent but cumulative across the fleet.

Remediation cost: **Easy** — read the digest from `docker inspect` and add `@sha256:…`; automate via Renovate or Dependabot.

### **Implementation principle**

- Target tech: Docker / OCI Dockerfile.
- Detection mechanism: sonar-iac Docker visitor (`org.sonar.iac.docker.tree.api.FromInstruction` — TBD).
- Algorithm:

  ```
  for each FromInstruction f in file:
      if f.image() does not contain "@sha256:": reportIssue(f, "Pin base image by digest")
  ```

- Known false positives: dynamic `ARG`-based `FROM ${REGISTRY}/${IMAGE}` — exempt when the variable expansion is itself digest-pinned via build args.
- Feasibility verdict: ✅
- Alternative tooling: Hadolint **DL3007**; Renovate/Dependabot automation for digest updates.
