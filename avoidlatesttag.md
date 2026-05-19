_(Proposed rule for creedengo-infra — 1031.)_

### **Rule title**

Do not use the `latest` tag (or implicit tag) on base images

### **Rule key**

1031

### **Language and platform**

Docker / OCI

### **Rule description**

`FROM node` (implicit `:latest`) or `FROM node:latest` ties the build to a moving target. The image content changes silently, breaking layer cache reuse, forcing re-pulls and invalidating digest-based verification. The runtime image picked at deploy time can also differ from the one tested in CI.

_Noncompliant Code Example_

```Dockerfile
FROM node
FROM ubuntu:latest
```

_Compliant Solution_

```Dockerfile
FROM node:20.11.1-alpine3.19
FROM ubuntu:22.04
```

### **Rule short description**

Every `FROM` must specify an explicit, immutable version tag — never `latest` or an implicit tag.

### **Rule justification**

**Why it matters**:
- Reproducibility: yesterday's `latest` is not today's `latest`.
- Cache thrashing: BuildKit cannot reuse layers when the parent image identity changes.

**Eco-design rationale** (axes 2·3·5):
- **Buildtime energy**: unstable parents invalidate all downstream layers — every CI run rebuilds the world. Stable tags can save **50–80 %** of CPU·s on incremental builds.
- **Runtime energy**: K8s nodes re-pull 100–500 MB whenever the daemon detects a new digest behind the moved tag — wasted bandwidth and decompression CPU.
- **Provisioning efficiency**: predictable pull time → faster scale-out under load (HPA, Karpenter).

Sources:
- https://docs.docker.com/develop/develop-images/guidelines/
- https://rules.sonarsource.com/docker/RSPEC-6596/
- https://github.com/hadolint/hadolint/wiki/DL3007

Measurement:

| Indicator | Method | Order of magnitude |
| --- | --- | --- |
| Cache miss rate in CI | BuildKit logs | -50 % to -80 % |
| Unplanned re-pulls | Registry metrics | -50 % to -90 % |

### **Severity / Remediation Cost**

Severity: **Major** — frequent root cause of energy-wasting "ghost rebuilds" and surprise re-pulls.

Remediation cost: **Easy** — replace `latest` (or no tag) with an explicit version.

### **Implementation principle**

- Target tech: Docker / OCI Dockerfile.
- Detection mechanism: sonar-iac Docker visitor (`org.sonar.iac.docker.tree.api.FromInstruction` — TBD).
- Algorithm:

  ```
  for each FromInstruction f:
      ref = f.image()
      tag = parseTag(ref) or "latest"
      if tag == "latest" or tag is null: reportIssue(f, "Avoid 'latest' / implicit tag")
  ```

- Known false positives: intentional rolling dev images — opt-out via `# eco-design:ignore`.
- Feasibility verdict: ✅
- Alternative tooling: Hadolint **DL3007**; SonarSource **RSPEC-6596** (may overlap — coordinate severity).
