_(Proposed rule for creedengo-infra — GCI1029. Extension of SonarSource RSPEC-6587.)_

### **Rule title**

Clean package manager caches inside the same RUN

### **Rule key**

GCI1029

### **Language and platform**

Docker / OCI

### **Rule description**

When a package manager downloads metadata or packages, it persists a cache (`/var/lib/apt/lists/*`, `/var/cache/apk/*`, `~/.cache/pip`, npm cache, yum/dnf cache, composer cache). If not removed in the same `RUN`, those caches are baked into the image layer, inflating the runtime image. SonarSource **RSPEC-6587** already covers `apt-get`; this rule **extends** detection to `apk`, `yum`, `dnf`, `pip`, `npm`, `composer`, `bundler`, `gem`.

_Noncompliant Code Example_

```Dockerfile
FROM python:3.11-alpine
RUN apk add --update build-base
RUN pip install -r requirements.txt
```

```Dockerfile
FROM amazonlinux:2023
RUN dnf install -y nginx
```

_Compliant Solution_

```Dockerfile
FROM python:3.11-alpine
RUN apk add --no-cache build-base
RUN pip install --no-cache-dir -r requirements.txt
```

```Dockerfile
FROM amazonlinux:2023
RUN dnf install -y nginx && dnf clean all && rm -rf /var/cache/dnf
```

### **Rule short description**

For every package install in a `RUN`, ensure the matching cache is either disabled (`--no-cache`/`--no-cache-dir`) or deleted within the same `RUN`.

### **Rule justification**

**Why it matters**:
- `apt-get` caches: 50–200 MB. `apk`: 20–80 MB. `pip`: 50–500 MB (wheels). `yum/dnf`: 100–600 MB.
- These layers are downloaded on every pod cold start, every node bootstrap, every cluster scale-up.

**Eco-design rationale** (axes 1·3·4·5·7):
- **Image size reduction**: -50 to -600 MB per layer, directly proportional to bandwidth saved on every pull.
- **Runtime energy**: every `docker pull` and registry round-trip is energy-hungry; cutting MB cuts kWh.
- **Memory efficiency**: smaller layers → smaller decompressed working set → less RAM in kubelet image cache.
- **Storage footprint**: registry billed per GB-month; on 50 microservices × 4 envs, a 200 MB saving = ~40 GB persistent reduction.
- **Provisioning efficiency**: scale-up latency (HPA, Karpenter, KEDA) dominated by image pull → smaller layers = faster scale-out, better demand/supply match, less over-provisioning.

Sources:
- https://rules.sonarsource.com/docker/RSPEC-6587/
- https://docs.docker.com/develop/develop-images/guidelines/
- https://wiki.alpinelinux.org/wiki/Alpine_Linux_package_management
- https://pip.pypa.io/en/stable/topics/caching/

Measurement:

| Indicator | Method | Order of magnitude |
| --- | --- | --- |
| Layer size diff | `docker history <image>` | -50 to -600 MB |
| Pull bandwidth | Registry metrics | linear with size delta |

### **Severity / Remediation Cost**

Severity: **Major** — cache leakage is a top contributor to image bloat.

Remediation cost: **Easy** — add the right flag or `&& rm -rf <cache>` at the end of the `RUN`.

### **Implementation principle**

- Target tech: Docker / OCI Dockerfile.
- Detection mechanism: sonar-iac Docker visitor (`org.sonar.iac.docker.tree.api.RunInstruction` — TBD).
- Algorithm:

  ```
  for each RunInstruction r in file:
      cmd = r.text()
      for (manager, installRegex, cleanCheck) in EXTENDED_MANAGERS:
          if installRegex matches cmd and not cleanCheck(cmd):
              reportIssue(r, "Clean " + manager + " cache in the same RUN")
  EXTENDED_MANAGERS:
    apk add        : require --no-cache OR rm -rf /var/cache/apk
    dnf install    : require dnf clean all AND rm -rf /var/cache/dnf
    yum install    : require yum clean all AND rm -rf /var/cache/yum
    pip install    : require --no-cache-dir OR rm -rf ~/.cache/pip
    npm install    : require npm cache clean --force OR --prefer-offline
    composer       : require composer clear-cache
    bundle install : require bundle config set --local clean true
  ```

- Known false positives: dev images intentionally keeping the cache (use `# eco-design:ignore`).
- Feasibility verdict: ✅ for the extension; defer `apt-get` to RSPEC-6587.
- Alternative tooling: Hadolint **DL3009/DL3019/DL3027**.
