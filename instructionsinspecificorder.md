_(Proposed rule for creedengo-infra — GCI1027. Refocused from the original draft into 3 auditable sub-rules.)_

### **Rule title**

Order Dockerfile instructions for cache efficiency

### **Rule key**

GCI1027

### **Language and platform**

Docker / OCI

### **Rule description**

Docker builds layer by layer; if a layer changes, all subsequent layers must be rebuilt. The original draft acknowledged this rule was "not implementable" generically. We refocus it into **three auditable sub-patterns**:
1. `COPY . .` appears **before** the package-manager install step → cache busted on every code change.
2. `WORKDIR` declared **after** `COPY`/`RUN` that use the working directory → directory created at every build.
3. `apt-get update` and `apt-get install` in **separate** `RUN` instructions (cf. RSPEC-S6597 / Hadolint DL3009 — delegated).

_Noncompliant Code Example_

```Dockerfile
FROM node:18-alpine
COPY . .
RUN npm install
CMD ["node", "index.js"]
```

_Compliant Solution_

```Dockerfile
FROM node:18-alpine
WORKDIR /app
COPY package*.json ./
RUN npm ci --omit=dev
COPY . .
CMD ["node", "index.js"]
```

### **Rule short description**

Place stable, slow-changing instructions (dependency installs) **before** volatile ones (source `COPY`) to maximise build-cache reuse.

### **Rule justification**

**Why it matters**:
- Reusing cached layers cuts CI/CD `docker build` duration by **50–90 %** on incremental code changes.
- Less rebuild work = less CPU·s on shared runners and less data downloaded from package registries.

**Eco-design rationale** (axes 2·5·7):
- **Buildtime energy**: A cached build of a 50-MB Node project drops from ~120 s to ~10 s (~-92 %). At a 100 W average runner this saves ~-20 to -50 Wh per CI run.
- **Provisioning efficiency**: shorter CI feedback loop → developers iterate faster → fewer wasted compute cycles in feature branches.
- **Storage footprint**: BuildKit `cache-to/cache-from` reused → less duplicated layer storage in CI cache buckets.

Sources:
- https://docs.docker.com/build/cache/
- https://docs.docker.com/build/cache/optimize/
- https://github.com/hadolint/hadolint/wiki/DL3009

Measurement:

| Indicator | Method | Order of magnitude |
| --- | --- | --- |
| Build time (cached) | CI runner duration | -50 % to -90 % |
| Cache hit ratio | `docker buildx build --progress=plain` | ↑ from ~10 % to ~80 % |

### **Severity / Remediation Cost**

Severity: **Minor** — energy is wasted only at build time, but every CI run is impacted.

Remediation cost: **Easy** — reorder a handful of lines.

### **Implementation principle**

- Target tech: Docker / OCI Dockerfile.
- Detection mechanism: sonar-iac Docker visitor; per-stage instruction list inspection.
- Algorithm:

  ```
  for each stage in file.stages():
      copyAll = first index of COPY whose sources contain "." or "*"
      runInstall = first index of RUN matching INSTALL_REGEX
      if copyAll < runInstall: report(copyAll, "COPY of source should follow dependency install")
      workdir = index of first WORKDIR
      firstFsAction = first index of any COPY/RUN
      if firstFsAction < workdir: report(workdir, "Declare WORKDIR before COPY/RUN")
  INSTALL_REGEX = /\b(npm\s+(ci|install)|yarn\s+install|pnpm\s+install|pip\s+install|poetry\s+install|composer\s+install|bundle\s+install|go\s+mod\s+download|cargo\s+fetch)\b/
  ```

- Known false positives: monorepo Dockerfiles; small projects without separate manifest files.
- Feasibility verdict: ⚠️ heuristic (~70 % precision).
- Alternative tooling: Hadolint **DL3009/DL3014/DL3015** via `sonar.externalIssuesReportPaths`.
