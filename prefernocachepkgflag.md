_(Proposed rule for creedengo-infra — GCI1037.)_

### **Rule title**

Use `--no-cache` / `--no-cache-dir` for package installs

### **Rule key**

GCI1037

### **Language and platform**

Docker / OCI

### **Rule description**

Most package managers expose a flag that disables their local cache entirely, removing the need for an explicit `rm -rf <cache>` afterwards. Using `apk add --no-cache`, `pip install --no-cache-dir`, `dnf install … && dnf clean all` keeps the layer minimal and the Dockerfile readable. This rule is a positive-form complement to `GCI1029` (cache cleaning) for the package managers where a no-cache flag is idiomatic.

_Noncompliant Code Example_

```Dockerfile
RUN apk add curl jq
RUN pip install requests
```

_Compliant Solution_

```Dockerfile
RUN apk add --no-cache curl jq
RUN pip install --no-cache-dir requests
```

### **Rule short description**

Enable the native no-cache flag on every supported package manager invocation.

### **Rule justification**

**Why it matters**:
- `apk` cache: 20–80 MB. `pip` cache: 50–500 MB. `npm` cache: 50–300 MB. `composer` cache: 50–200 MB.
- Removing the flag saves a `RUN` and a layer over the cleanup approach.

**Eco-design rationale** (axes 1·3·5·7):
- **Image size reduction**: typically -20 to -500 MB per impacted layer.
- **Runtime energy**: smaller layers → faster pulls → less network/CPU at cold start.
- **Provisioning efficiency**: faster scale-out events for HPA/Karpenter/KEDA.
- **Storage footprint**: registry billed per GB-month.

Sources:
- https://wiki.alpinelinux.org/wiki/Alpine_Linux_package_management
- https://pip.pypa.io/en/stable/topics/caching/
- https://docs.npmjs.com/cli/v10/commands/npm-install

Measurement:

| Indicator | Method | Order of magnitude |
| --- | --- | --- |
| Layer size | `docker history` | -20 to -500 MB |
| Pull duration | `docker pull` timing | -X seconds per pod cold start |

### **Severity / Remediation Cost**

Severity: **Minor** — easy to fix but very widespread.

Remediation cost: **Easy** — add a single flag.

### **Implementation principle**

- Target tech: Docker / OCI Dockerfile.
- Detection mechanism: sonar-iac Docker visitor.
- Algorithm:

  ```
  for each RunInstruction r:
      if r contains "apk add"      and not "--no-cache":     report(r)
      if r contains "pip install"  and not "--no-cache-dir": report(r)
      if r contains "npm install"  and not ("--prefer-offline" or "npm cache clean"): report(r)
      if r contains "composer install" and not "--no-cache": report(r)
  ```

- Known false positives: multi-line RUNs that perform a manual `rm -rf` immediately afterwards — exclude via `cleanCheck` (same logic as GCI1029).
- Feasibility verdict: ✅
- Alternative tooling: Hadolint **DL3019/DL3013**.
