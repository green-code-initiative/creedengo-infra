_(Proposed rule for creedengo-infra — 1025. Merges former `multistagebuilddocker.md`.)_

### **Rule title**

Use Docker multi-stage build

### **Rule key**

1025

### **Language and platform**

Docker / OCI

### **Rule description**

Multi-stage builds separate the **build toolchain** (compilers, package managers, dev dependencies) from the **runtime image**. Without multi-stage, the final image embeds gigabytes of artefacts (`mvn`, `gcc`, `npm`, `go`, `cargo`, `pip`, …) that are useless at runtime, inflate the registry, slow down every cold start and consume extra energy at each pod scheduling event.

_Noncompliant Code Example_ (Maven)

```Dockerfile
FROM maven:3.9-eclipse-temurin-17
COPY ./ ./
RUN mvn clean package
CMD ["java", "-jar", "target/app.jar"]
```

_Noncompliant Code Example_ (Node.js)

```Dockerfile
FROM node:lts
COPY . .
RUN npm install && npm run build
CMD ["node", "index.js"]
```

_Compliant Solution_ (Maven)

```Dockerfile
FROM maven:3.9-eclipse-temurin-17 AS builder
COPY ./ ./
RUN mvn -B -ntp clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
COPY --from=builder /target/app.jar /app.jar
CMD ["java", "-jar", "/app.jar"]
```

_Compliant Solution_ (Go on `scratch`)

```Dockerfile
FROM golang:1.22 AS builder
WORKDIR /src
COPY . .
RUN CGO_ENABLED=0 go build -o /out/app ./cmd/app

FROM scratch
COPY --from=builder /out/app /app
ENTRYPOINT ["/app"]
```

### **Rule short description**

Split your Dockerfile into a build stage and a slim runtime stage; only the artefact required to run the app must reach the final image.

### **Rule justification**

**Why it matters**:
- Final images can shrink by **90–97 %** (cf. ResearchSquare 2023 study below): 1.47 GB → 41.6 MB.
- Fewer layers = faster `docker pull` on every node, faster cold start, less RAM cached on the kubelet.
- Smaller attack surface (no compilers shipped in production).

**Eco-design rationale** (axes 1·2·3·4·5·7):
- **Image size reduction**: -50 % to -97 % final image weight → less GB pulled across the fleet at every rollout. On a 100-node cluster pulling a 1 GB image, going to 50 MB saves ~95 GB of bandwidth per rollout.
- **Buildtime energy**: reuses the `builder` layer cache → CI/CD `docker build` ~30–50 % faster on average (estimated). Less CPU·s on shared runners (Green Software Foundation SCI proxy).
- **Runtime energy**: smaller images → faster cold start (-30 % to -70 % observed on K8s with `imagePullPolicy: Always`), less RAM page cache pressure on nodes.
- **Memory efficiency**: runtime image without `mvn` / `npm` / `gcc` releases hundreds of MB of kubelet image cache per node.
- **Provisioning efficiency**: autoscaling events (HPA, Karpenter) propagate faster; scale-up latency is dominated by image pull time.
- **Storage footprint**: registry storage (ECR/GCR/Artifactory) is billed per GB-month; reducing 1 GB on an image deployed in 5 environments × 3 regions ≈ 15 GB saved continuously.

Sources:
- https://docs.docker.com/build/building/multi-stage/
- https://docs.docker.com/develop/develop-images/guidelines/
- https://assets-eu.researchsquare.com/files/rs-3276965/v1_covered_8dc408b5-6997-486a-89c8-a5c66fddf60e.pdf
- https://greensoftware.foundation/articles/sci-specification

Measurement:

| Indicator | Method | Order of magnitude |
| --- | --- | --- |
| Final image size | `docker images --format '{{.Size}}'` | 1.47 GB → 41.6 MB (-97.17 %) |
| Build duration | `docker buildx build --progress=plain` | -30 to -50 % wall time on cached builds |
| Cold start latency | `kubectl get events --field-selector reason=Pulled` | -30 to -70 % image pull duration |
| Registry bandwidth | Registry metrics (ECR `BytesDownloaded`) | scales linearly with size reduction |

### **Severity / Remediation Cost**

Severity: **Major** — runtime images bloated by build toolchain waste storage, bandwidth and energy on every pull/cold start.

Remediation cost: **Medium** — refactor Dockerfile into 2+ stages, often requires understanding which artefact to `COPY --from=builder`.

### **Implementation principle**

- Target tech: Docker / OCI Dockerfile.
- Detection mechanism: sonar-iac Docker visitor (`org.sonar.iac.docker.tree.api.{File,FromInstruction,RunInstruction,CmdInstruction,EntrypointInstruction}` — API coordinates TBD against `${version.sonar-iac}` pinned in `pom.xml`).
- Algorithm (pseudo-code):

  ```
  fromInstructions = file.body().instructions().filter(FromInstruction)
  if size(fromInstructions) < 2:
      lastFromIdx = index(last FromInstruction)
      runsAfter = instructions().after(lastFromIdx).filter(RunInstruction)
      if any run.text matches BUILD_CMD_REGEX and a CMD/ENTRYPOINT exists after it:
          reportIssue(run, "Use a multi-stage build to separate build from runtime")
  BUILD_CMD_REGEX = /\b(mvn|gradle|ant|npm|yarn|pnpm|gcc|g\+\+|make|cmake|go\s+build|cargo\s+build|dotnet\s+(publish|build)|pip\s+install|poetry\s+install|bundle\s+install|composer\s+install)\b/
  ```

- Known false positives: CI-only images intended to *run* the build (no `CMD`/`ENTRYPOINT` consumer).
- Feasibility verdict: ✅
- Alternative tooling: Hadolint **DL3007** (latest tag) + **DL3008/DL3009** complement; cross-check with Trivy image size diff in CI.
