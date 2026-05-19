_(Proposed rule for creedengo-infra — 1058.)_

### **Rule title**

CI workflows must cache dependencies

### **Rule key**

1058

### **Language and platform**

GitHub Actions / GitLab CI / Azure DevOps / Jenkinsfile

### **Rule description**

A CI job that re-downloads Maven, Gradle, npm, pnpm, pip or Go modules **on every run** wastes shared runner CPU·s, network bandwidth and registry egress. Every major CI platform provides a cache action (`actions/cache`, GitLab `cache:`, Azure Pipelines `Cache@2`, Jenkins `cache(...)`). This rule flags workflow files where a build step is detected without a corresponding cache step.

_Noncompliant Code Example_

```yaml
# .github/workflows/build.yml
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
      - run: npm ci
      - run: npm test
# No actions/cache step → npm re-downloads all dependencies every run.
```

_Compliant Solution_

```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with: { node-version: 20, cache: npm }   # built-in cache
      - run: npm ci
      - run: npm test
```

### **Rule short description**

A workflow that calls a package manager (`mvn`, `gradle`, `npm`, `pnpm`, `pip`, `poetry`, `go mod`) must enable the matching cache.

### **Rule justification**

**Why it matters**:
- npm/pnpm re-downloads can take 30–120 s and 50–500 MB per CI run.
- On a repo with 50 PRs/day, this can mean ~1 hour of CPU·s and 10 GB of egress saved daily by caching.

**Eco-design rationale** (axes 2·5·7):
- **Buildtime energy**: -30 % to -70 % wall time on package-install phase.
- **Storage footprint**: registry egress quota saved; less data crossing the public internet.
- **Provisioning efficiency**: faster feedback loop → fewer concurrent runners needed.

Sources:
- https://docs.github.com/en/actions/using-workflows/caching-dependencies-to-speed-up-workflows
- https://docs.gitlab.com/ee/ci/caching/
- https://learn.microsoft.com/en-us/azure/devops/pipelines/release/caching

Measurement:

| Indicator | Method | Order of magnitude |
| --- | --- | --- |
| Job wall time | CI logs | -30 % to -70 % |
| Network egress | runner metrics | -50 % to -90 % |

### **Severity / Remediation Cost**

Severity: **Minor** — easy to fix, cumulative impact.

Remediation cost: **Easy** — add 3-5 YAML lines.

### **Implementation principle**

- Target tech: YAML workflows.
- Detection mechanism: custom Sensor analysing `.github/workflows/*.yml`, `.gitlab-ci.yml`, `azure-pipelines.yml`, `Jenkinsfile`.
- Algorithm:

  ```
  for each workflow file:
      jobs = parsed jobs
      for each job:
          steps = job steps
          if any step.run matches BUILD_REGEX:
              if no cache step targets the matching package manager:
                  report(step, "Add a cache step for " + manager)
  BUILD_REGEX = same as 1025 INSTALL_REGEX
  ```

- Known false positives: workflows running on self-hosted runners with persistent volumes — opt-out via comment `# eco-design:ignore`.
- Feasibility verdict: ⚠️ (custom Sensor; YAML is multi-schema).
- Alternative tooling: actionlint; GitLab `policy:cache`.
