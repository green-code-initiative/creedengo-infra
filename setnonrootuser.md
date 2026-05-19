_(Proposed rule for creedengo-infra — GCI1035.)_

### **Rule title**

Run containers as a non-root `USER`

### **Rule key**

GCI1035

### **Language and platform**

Docker / OCI

### **Rule description**

A container that runs as `root` is the canonical pattern that triggers CVE-driven emergency re-deployments: any kernel/runtime CVE forces immediate re-build, re-push, re-pull and re-deploy across the fleet. Declaring an explicit non-root `USER` lowers this risk and cuts down the frequency of unplanned re-pushes — each of which costs CPU·s, network bandwidth and operator time.

_Noncompliant Code Example_

```Dockerfile
FROM eclipse-temurin:17-jre-alpine
COPY app.jar /app.jar
CMD ["java", "-jar", "/app.jar"]
```

_Compliant Solution_

```Dockerfile
FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S app && adduser -S app -G app
COPY --chown=app:app app.jar /app.jar
USER app:app
CMD ["java", "-jar", "/app.jar"]
```

### **Rule short description**

The last stage of every Dockerfile must declare a non-root `USER`.

### **Rule justification**

**Why it matters**:
- Root containers magnify the blast radius of every CVE → more emergency redeploys.
- Many orchestrators (OpenShift, GKE Autopilot, kube-green hardened defaults) reject root containers and waste retry cycles.

**Eco-design rationale** (axes 2·3·5):
- **Buildtime energy**: fewer urgent CVE rebuilds → fewer wasted CI minutes (each emergency rebuild ≈ 5–30 min of CI on shared runners).
- **Runtime energy**: fewer fleet-wide re-pulls of patched images; one rolling redeploy on a 100-pod service can move 50–300 GB across the network.
- **Provisioning efficiency**: avoids retry storms when an admission controller rejects root pods.

Sources:
- https://docs.docker.com/develop/develop-images/dockerfile_best-practices/#user
- https://kubernetes.io/docs/concepts/security/pod-security-standards/
- https://github.com/hadolint/hadolint/wiki/DL3002

Measurement:

| Indicator | Method | Order of magnitude |
| --- | --- | --- |
| Emergency CVE rebuilds | CI history | -X per quarter (org-specific) |
| Rolling redeploy bandwidth | Registry metrics | saved on each avoided CVE rollout |

### **Severity / Remediation Cost**

Severity: **Major** — direct security and indirect eco-design impact.

Remediation cost: **Easy** — add a `USER` line and `--chown` on `COPY`.

### **Implementation principle**

- Target tech: Docker / OCI Dockerfile.
- Detection mechanism: sonar-iac Docker visitor.
- Algorithm:

  ```
  lastStage = last build stage of file
  userInstructions = lastStage.instructions().filter(UserInstruction)
  if userInstructions is empty or last user resolves to "root"/"0":
      reportIssue(lastStage, "Declare a non-root USER in the final stage")
  ```

- Known false positives: privileged system containers (CNI plugins, host agents) — exempt via `# eco-design:ignore root`.
- Feasibility verdict: ✅
- Alternative tooling: Hadolint **DL3002**, Trivy security scan.
