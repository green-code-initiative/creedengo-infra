_(Proposed rule for creedengo-infra — GCI1039.)_

### **Rule title**

Prefer static binaries on `scratch`/`distroless` for compiled languages

### **Rule key**

GCI1039

### **Language and platform**

Docker / OCI

### **Rule description**

Go, Rust, Zig and statically-linked C/C++ binaries can run on a `FROM scratch` or `FROM gcr.io/distroless/static` image of ~2–20 MB. Many projects unnecessarily ship them on `alpine`/`debian-slim` images of 50–150 MB. For compiled-to-static languages, the runtime base image is dead weight.

_Noncompliant Code Example_

```Dockerfile
FROM golang:1.22 AS builder
WORKDIR /src
COPY . .
RUN go build -o /out/app ./cmd/app

FROM alpine:3.19
COPY --from=builder /out/app /usr/local/bin/app
ENTRYPOINT ["/usr/local/bin/app"]
# final image: ~12 MB
```

_Compliant Solution_

```Dockerfile
FROM golang:1.22 AS builder
WORKDIR /src
COPY . .
RUN CGO_ENABLED=0 go build -ldflags="-s -w" -o /out/app ./cmd/app

FROM gcr.io/distroless/static-debian12:nonroot
COPY --from=builder /out/app /usr/local/bin/app
ENTRYPOINT ["/usr/local/bin/app"]
# final image: ~3 MB
```

### **Rule short description**

When the build stage produces a static binary (Go/Rust/Zig), the runtime stage should be `scratch`, `gcr.io/distroless/static`, or equivalent.

### **Rule justification**

**Why it matters**:
- A 3 MB image vs a 50 MB image is a 16× reduction in stored, transferred and cached bytes.
- No package manager in the runtime → drastically reduced CVE surface.

**Eco-design rationale** (axes 1·3·4·5·7):
- **Image size reduction**: typically -80 % to -97 %.
- **Runtime energy**: cold start in 100–300 ms instead of 1–3 s on serverless platforms; less CPU·s on every pull, every decompression.
- **Memory efficiency**: no shell, no libc layer cached on the node beyond what is already in the kernel.
- **Scale-to-zero**: enables fast cold starts on Knative/Cloud Run/Fargate, allowing aggressive scale-to-zero policies and matching the SCI principle of "use as little as possible".
- **Storage footprint**: registry storage and node disk usage drop linearly with image size.

Sources:
- https://github.com/GoogleContainerTools/distroless
- https://docs.docker.com/develop/develop-images/multistage-build/
- https://go.dev/blog/distroless

Measurement:

| Indicator | Method | Order of magnitude |
| --- | --- | --- |
| Final image size | `docker images` | -80 % to -97 % |
| Cold start | Knative/Cloud Run logs | -50 % to -85 % |

### **Severity / Remediation Cost**

Severity: **Info** — applicable mainly to Go/Rust/Zig services.

Remediation cost: **Hard** — requires verifying the binary is truly static (`CGO_ENABLED=0`, no dynamic linking), and that no shell/debugger is needed in production.

### **Implementation principle**

- Target tech: Docker / OCI Dockerfile.
- Detection mechanism: sonar-iac Docker visitor (heuristic, language-specific).
- Algorithm:

  ```
  if file has a builder stage whose FROM matches /^golang:|^rust:|^ekidd\/rust-musl|^messense\/rust-musl/:
      lastFrom = last FromInstruction
      if lastFrom.image() not in {scratch, gcr.io/distroless/static*, busybox:musl}:
          reportIssue(lastFrom, "Static binary detected — prefer scratch or distroless/static")
  ```

- Known false positives: Go binaries with CGO enabled or Rust with dynamic OpenSSL — exempt when `CGO_ENABLED=1` or `RUSTFLAGS` opts in to dynamic linking.
- Feasibility verdict: ⚠️ heuristic (~70 % precision).
- Alternative tooling: kube-linter image-size policy; manual review.
