_(Proposed rule for creedengo-infra — GCI1060.)_

### **Rule title**

Pin CI runner versions and prefer ARM runners

### **Rule key**

GCI1060

### **Language and platform**

GitHub Actions / GitLab CI / Azure DevOps

### **Rule description**

Two small but cumulative eco-design improvements for CI:
1. **Pin runner images** (`ubuntu-22.04` instead of `ubuntu-latest`): avoids surprise migrations that invalidate caches and re-pull base toolchains.
2. **Use ARM runners** (GitHub `ubuntu-22.04-arm`, GitLab SaaS ARM runners) when the build supports it: ARM runners are typically 30–40 % cheaper and consume proportionally less energy.

_Noncompliant Code Example_

```yaml
jobs:
  build:
    runs-on: ubuntu-latest   # moving target; x86 default
    steps: [...]
```

_Compliant Solution_

```yaml
jobs:
  build:
    runs-on: ubuntu-22.04-arm   # pinned + ARM
    steps: [...]
```

### **Rule short description**

`runs-on` (or equivalent) should be pinned to a specific version, and use ARM when the build supports it.

### **Rule justification**

**Why it matters**:
- `ubuntu-latest` migrations break caches, re-download toolchains and invalidate Docker layer caches.
- ARM runners deliver more CI minutes per kWh.

**Eco-design rationale** (axes 2·5):
- **Buildtime energy**: pinned runner = stable cache layer = 30–50 % faster on subsequent runs.
- **Runtime energy (of the runner pool)**: ARM Graviton-based GitHub runners report up to 40 % better perf/watt.

Sources:
- https://docs.github.com/en/actions/using-github-hosted-runners/about-github-hosted-runners
- https://github.blog/2024-06-03-arm64-on-github-actions-powering-faster-more-efficient-build-systems/

Measurement:

| Indicator | Method | Order of magnitude |
| --- | --- | --- |
| Cache miss rate | CI logs | -X % after pinning |
| Build wall time | CI logs | -20 % to -40 % on ARM |

### **Severity / Remediation Cost**

Severity: **Info**.

Remediation cost: **Easy** — change `runs-on` value.

### **Implementation principle**

- Target tech: YAML workflows.
- Detection mechanism: custom Sensor.
- Algorithm:

  ```
  for each workflow.jobs[*].runs-on:
      if value contains "latest": report("Pin runner version (e.g., ubuntu-22.04)")
      if value is generic x86 image AND repo has multi-arch capability flag: report (Info)
  ```

- Known false positives: workflows requiring x86 native binaries — exclude when `# eco-design:arch=x86` is present.
- Feasibility verdict: ⚠️ (custom Sensor).
- Alternative tooling: actionlint.
