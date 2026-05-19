_(Proposed rule for creedengo-infra — 1028.)_

### **Rule title**

Provide and curate a `.dockerignore` file

### **Rule key**

1028

### **Language and platform**

Docker / OCI

### **Rule description**

The Docker build context is sent in full to the daemon before any instruction is executed. Without a `.dockerignore`, projects routinely ship `node_modules/`, `target/`, `.git/`, `.venv/`, build artefacts, IDE files, secrets and local databases into the build context — even when the Dockerfile does not `COPY` them. This bloats the build context, leaks secrets, and slows down every build.

_Noncompliant Code Example_

```text
my-app/
  Dockerfile
  node_modules/   (380 MB)
  target/         (140 MB)
  .git/           (90 MB)
  .env            (secrets)
  src/
# No .dockerignore present.
```

_Compliant Solution_

```gitignore
# .dockerignore (Node.js)
node_modules
npm-debug.log*
.git
.gitignore
.dockerignore
Dockerfile*
.env
.env.*
*.log
coverage/
dist/
build/
.idea/
.vscode/
```

```gitignore
# .dockerignore (Maven)
sonar-plugin/target/
.mvn/wrapper/maven-wrapper.jar
.git
*.iml
.idea/
.env*
```

### **Rule short description**

Every Dockerfile must be accompanied by a curated `.dockerignore` excluding VCS metadata, dependency caches, secrets and build outputs.

### **Rule justification**

**Why it matters**:
- A Node.js project without `.dockerignore` uploads **400+ MB** of `node_modules` — 20× the size of an alpine image.
- Secrets in `.env` or `.git/config` can end up in image layers if a permissive `COPY . .` is used.

**Eco-design rationale** (axes 1·2·5·7):
- **Buildtime energy**: 600 MB → 5 MB context cuts the "sending build context" phase from ~30 s to <1 s.
- **Image size reduction**: prevents accidental large `COPY .` instructions pulling junk into the image.
- **Storage footprint**: build cache layers stop snapshotting stale `node_modules`, freeing several GB on CI runners.
- **Provisioning efficiency**: shorter context upload → faster CI builds → faster deploys → less idle cloud-runner time.

Sources:
- https://docs.docker.com/build/building/context/#dockerignore-files
- https://www.ncbi.nlm.nih.gov/pmc/articles/PMC7654784/
- https://docs.docker.com/develop/develop-images/guidelines/

Measurement:

| Indicator | Method | Order of magnitude |
| --- | --- | --- |
| Build context size | `docker build` "Sending build context" line | -90 % to -99 % |
| Context upload time | CI build logs | from ~30 s to <1 s |

### **Severity / Remediation Cost**

Severity: **Major** — frequent root cause of huge build contexts and leaked secrets.

Remediation cost: **Easy** — add a `.dockerignore` with a per-stack template.

### **Implementation principle**

- Target tech: filesystem (`.dockerignore` files), not Docker AST.
- Detection mechanism: custom `Sensor` (`DockerignoreSensor implements Sensor`):
  1. Locate every `**/Dockerfile*` in the project.
  2. For each Dockerfile, look up a sibling `.dockerignore` in the same directory or first parent.
  3. If absent → raise Major issue on the Dockerfile.
  4. If present, parse line-by-line and check against a per-stack template inferred from the last `FROM`.
- Algorithm:

  ```
  for each dockerfile in project:
      ignore = nearest(".dockerignore", from=dockerfile.dir())
      if ignore is null: report(dockerfile, "Missing .dockerignore")
      else:
          stack = inferStack(dockerfile.lastFrom().image())
          missing = TEMPLATE[stack] - ignore.entries()
          if missing not empty: report(ignore, "Missing entries: " + missing)
  ```

- Known false positives: monorepo with a single root `.dockerignore` for multiple Dockerfiles → fallback search to project root.
- Feasibility verdict: ⚠️ (custom Sensor required, not in sonar-iac Docker AST).
- Alternative tooling: Trivy secret scan on built images as runtime safety net.
