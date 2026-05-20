_(Proposed rule for creedengo-infra — 1033.)_

### **Rule title**

Prefer `COPY` over `ADD` for local files

### **Rule key**

1033

### **Language and platform**

Docker / OCI

### **Rule description**

`ADD` has two side effects beyond simple file copy: it auto-extracts local archives and (historically) downloads remote URLs. These behaviours are surprising, harder to cache, and often pull more data than needed. Use `COPY` for plain file copies. For remote downloads, use a dedicated `RUN curl/wget … && verify checksum`, so the network step is explicit, cacheable and auditable.

_Noncompliant Code Example_

```Dockerfile
ADD https://example.com/app.tar.gz /opt/
ADD ./app /usr/src/app
```

_Compliant Solution_

```Dockerfile
COPY ./app /usr/src/app
RUN curl -fsSL -o /tmp/app.tar.gz https://example.com/app.tar.gz \
 && echo "<sha256>  /tmp/app.tar.gz" | sha256sum -c - \
 && tar -xzf /tmp/app.tar.gz -C /opt/ \
 && rm /tmp/app.tar.gz
```

### **Rule short description**

Use `COPY` for local files and an explicit `RUN curl … && verify` for remote artefacts.

### **Rule justification**

**Why it matters**:
- `ADD` auto-extracts archives, leaking files into the image even when not desired.
- Remote URLs with `ADD` are not cached deterministically and can re-download silently.

**Eco-design rationale** (axes 1·2·5):
- **Image size reduction**: explicit `tar -xz … && rm` deletes the archive within the same layer; `ADD` keeps it as a hidden source of bloat.
- **Buildtime energy**: deterministic cache → fewer redundant network fetches on every CI build.
- **Provisioning efficiency**: predictable build outputs accelerate downstream deployment pipelines.

Sources:
- https://docs.docker.com/develop/develop-images/guidelines/#use-copy-instead-of-add
- https://github.com/hadolint/hadolint/wiki/DL3020
- https://github.com/hadolint/hadolint/wiki/DL3022

Measurement:

| Indicator | Method | Order of magnitude |
| --- | --- | --- |
| Image size | `docker images` | -10 to -100 MB per `ADD` archive cleaned |
| Cache hit ratio | BuildKit | ↑ measurable on repeated builds |

### **Severity / Remediation Cost**

Severity: **Minor** — small individual impact but a frequent anti-pattern.

Remediation cost: **Easy** — replace `ADD` with `COPY` or explicit `RUN curl`.

### **Implementation principle**

- Target tech: Docker / OCI Dockerfile.
- Detection mechanism: sonar-iac Docker visitor (`AddInstruction`).
- Algorithm:

  ```
  for each AddInstruction a:
      if a.sources() are all local plain files (no URL, no tar/zip extension):
          reportIssue(a, "Use COPY instead of ADD for local files")
      elif a.sources() contain a URL:
          reportIssue(a, "Use explicit RUN curl with checksum verification instead of ADD URL")
  ```

- Known false positives: legitimate `ADD --chown` of a tarball when extraction is desired (allowlist by comment).
- Feasibility verdict: ✅
- Alternative tooling: Hadolint **DL3020/DL3022**.
