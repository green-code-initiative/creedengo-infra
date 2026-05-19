_(Proposed rule for creedengo-infra — GCI1026.)_

### **Rule title**

Use a lightweight Docker base image

### **Rule key**

GCI1026

### **Language and platform**

Docker / OCI

### **Rule description**

Heavy general-purpose base images (`ubuntu`, `centos`, `python:3.x`, `node:18`) embed hundreds of packages your application never uses. Prefer minimal variants: `-alpine`, `-slim`, `distroless`, `scratch`, `chiseled`, `wolfi`, `busybox`, `minideb`. Apply this **on the last `FROM`**, since only that image is shipped.

_Noncompliant Code Example_

```Dockerfile
FROM python:3.11
# image size: ~1.0 GB
```

```Dockerfile
FROM node:18
# image size: ~352 MB
```

```Dockerfile
FROM azul/zulu-openjdk:21
# image size: ~190 MB
```

_Compliant Solution_

```Dockerfile
FROM python:3.11-slim
# image size: ~125 MB
```

```Dockerfile
FROM node:18-alpine
# image size: ~65 MB
```

```Dockerfile
FROM gcr.io/distroless/java21-debian12
# image size: ~153 MB
```

### **Rule short description**

The final `FROM` in a Dockerfile must use a minimal base image (alpine/slim/distroless/scratch/chiseled/wolfi/busybox) unless an explicit waiver is documented.

### **Rule justification**

**Why it matters**:
- A `python:3.8` image (356 MB) replaced by `python:3.8-alpine` (17 MB) saves **~95 %** of the image weight — verified on Docker Hub.
- Around **40 %** of the 1000 most popular Docker Hub images offer a `slim`/`alpine` variant.
- Fewer packages = fewer CVEs to patch = fewer re-builds and re-deploys.

**Eco-design rationale** (axes 1·3·4·5·7):
- **Image size reduction**: typical -70 % to -95 % final size.
- **Runtime energy**: smaller working set on the kubelet, faster `docker pull` on node bootstrap → less network I/O, less CPU·s spent decompressing layers.
- **Memory/CPU efficiency**: alpine variants use musl libc and BusyBox, reducing baseline process RAM by 5–20 MB per container. On a 1000-pod cluster, this saves several GB of RAM.
- **Provisioning efficiency**: cold starts in serverless container platforms (Cloud Run, ACI, Fargate, Knative) are linear in image pull time; switching to slim variants typically cuts start latency by 30–60 %.
- **Storage footprint**: registry billing (ECR ~$0.10/GB-month) and node disk pressure linearly proportional to image size.

Sources:
- https://docs.docker.com/develop/develop-images/guidelines/
- https://github.com/GoogleContainerTools/distroless
- https://www.fullstack.com/labs/resources/blog/small-is-beautiful-how-container-size-impacts-deployment-and-resource-usage
- https://aws.amazon.com/architecture/well-architected/sustainability/

Measurement:

| Indicator | Method | Order of magnitude |
| --- | --- | --- |
| Image size | `docker images <img>` | -70 % to -95 % |
| Cold start | Cloud Run / Fargate logs | -30 % to -60 % |
| Registry storage | Registry billing per GB-month | linear with size delta |

### **Severity / Remediation Cost**

Severity: **Major** — large base image is one of the top contributors to image bloat.

Remediation cost: **Easy** — change one tag; runtime behaviour rarely affected (validate libc compatibility for alpine).

### **Implementation principle**

- Target tech: Docker / OCI Dockerfile.
- Detection mechanism: sonar-iac Docker visitor (`org.sonar.iac.docker.tree.api.{File,FromInstruction}` — TBD).
- Algorithm:

  ```
  lastFrom = last FromInstruction in file
  image = lastFrom.image()
  repo, tag = split(image)
  if tag in LIGHT_TAGS or repo contains {distroless,scratch,busybox,wolfi,chiseled,minideb}: ok
  else: reportIssue(lastFrom, "Use a slim/alpine/distroless variant on the final FROM")
  LIGHT_TAGS: alpine, slim, slim-bookworm, alpine3.*, distroless, scratch, chiseled, wolfi, micro, busybox
  ```

- Known false positives: corporate private registries that don't follow the `slim/alpine` naming convention → allowlist via Sonar property `creedengo.infra.lightweight.allowlist`.
- Feasibility verdict: ✅
- Alternative tooling: Hadolint **DL3006** + Trivy size reporting.
