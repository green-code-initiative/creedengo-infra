_(Proposed rule for creedengo-infra — 1036.)_

### **Rule title**

Strip build tools from the runtime image

### **Rule key**

1036

### **Language and platform**

Docker / OCI

### **Rule description**

Compilers, package managers and SDKs are needed at build time but useless at runtime. Their presence in the final image (`gcc`, `make`, `git`, `mvn`, `npm`, `python-dev`, `build-essential`, `kernel-headers`, …) inflates the layer by 100 MB to 1 GB, drags every cold start, increases CVE surface and triggers needless re-pulls. This rule complements multi-stage builds (`1025`) by detecting explicit installs that should never reach the runtime stage.

_Noncompliant Code Example_

```Dockerfile
FROM eclipse-temurin:17-jre-alpine
RUN apk add --no-cache gcc make musl-dev git
COPY app.jar /app.jar
CMD ["java", "-jar", "/app.jar"]
```

_Compliant Solution_

```Dockerfile
FROM eclipse-temurin:17-jdk-alpine AS builder
RUN apk add --no-cache gcc make
COPY . /src
RUN cd /src && ./build.sh

FROM eclipse-temurin:17-jre-alpine
COPY --from=builder /sonar-plugin/src/app.jar /app.jar
CMD ["java", "-jar", "/app.jar"]
```

### **Rule short description**

The final stage must not install or contain build toolchains (gcc, make, git, mvn, npm, sdkmans, headers).

### **Rule justification**

**Why it matters**:
- Build tools commonly add 100–800 MB to a runtime image.
- They expand the attack surface significantly (kernel headers, source compilers).

**Eco-design rationale** (axes 1·3·4·5·7):
- **Image size reduction**: -100 MB to -800 MB on the final image.
- **Runtime energy**: every pod cold start and node bootstrap saves the corresponding decompression CPU and bandwidth.
- **Memory efficiency**: kubelet image cache holds smaller layers → better packing on tight nodes.
- **Provisioning efficiency**: HPA / KEDA scale-up reacts faster to load spikes.
- **Storage footprint**: registry billed per GB-month; the reduction is permanent across all environments.

Sources:
- https://docs.docker.com/develop/develop-images/guidelines/
- https://docs.docker.com/build/building/multi-stage/
- https://github.com/hadolint/hadolint/wiki/DL3015

Measurement:

| Indicator | Method | Order of magnitude |
| --- | --- | --- |
| Final image size | `docker history <image>` | -100 to -800 MB |
| CVE count | Trivy scan | -10 % to -40 % |

### **Severity / Remediation Cost**

Severity: **Major** — large, recurring waste.

Remediation cost: **Medium** — refactor into multi-stage (see 1025).

### **Implementation principle**

- Target tech: Docker / OCI Dockerfile.
- Detection mechanism: sonar-iac Docker visitor.
- Algorithm:

  ```
  lastStage = last build stage
  for each RunInstruction r in lastStage:
      if r.text() installs BUILD_TOOL_PACKAGES (apt/apk/dnf/yum/pip):
          reportIssue(r, "Move build tools to a separate builder stage")
  BUILD_TOOL_PACKAGES: gcc, g++, make, cmake, build-essential, musl-dev, alpine-sdk,
                      libc-dev, linux-headers, git, mvn, gradle, npm, yarn, pip, poetry,
                      python3-dev, jdk, sdk, go (when in runtime stage)
  ```

- Known false positives: pure build images (no runtime stage downstream) — opt-out via image label or comment.
- Feasibility verdict: ✅
- Alternative tooling: Hadolint **DL3015**; Trivy footprint diff.
