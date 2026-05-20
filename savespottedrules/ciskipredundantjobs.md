_(Proposed rule for creedengo-infra — 1059.)_

### **Rule title**

CI: skip redundant jobs (paths filter, `concurrency`, draft PRs)

### **Rule key**

1059

### **Language and platform**

GitHub Actions / GitLab CI / Azure DevOps

### **Rule description**

Many CI pipelines run a full build/test matrix on every commit — including commits that only touch docs or markdown files, draft PR pushes superseded seconds later, and force-pushes. Three patterns dramatically reduce the wasted CPU·s:
1. `paths` / `paths-ignore` filters to skip jobs unrelated to changes.
2. `concurrency` (GitHub) / `interruptible: true` (GitLab) to cancel an in-flight run when a new commit arrives.
3. Conditional skipping on draft PRs (`if: github.event.pull_request.draft == false`).

_Noncompliant Code Example_

```yaml
on:
  push: { branches: [main] }
  pull_request:
# No paths filter, no concurrency, runs on every draft commit.
jobs:
  full-matrix: {...}
```

_Compliant Solution_

```yaml
on:
  push:
    branches: [main]
    paths: ['src/**','pom.xml','Dockerfile']
  pull_request:
    paths: ['src/**','pom.xml','Dockerfile']

concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true

jobs:
  full-matrix:
    if: github.event.pull_request.draft == false
    {...}
```

### **Rule short description**

Long-running CI jobs should declare `paths` filters, a `concurrency` group with `cancel-in-progress`, and skip draft PRs.

### **Rule justification**

**Why it matters**:
- Docs-only changes triggering a 20-minute build/test matrix is the single biggest pure-waste category in CI.
- A typical organisation can cut CI minutes by 20–50 % by adopting all three patterns.

**Eco-design rationale** (axes 2·5·6·7):
- **Buildtime energy**: direct reduction in CPU·s consumed on shared runners.
- **Provisioning efficiency**: fewer concurrent runners needed → smaller self-hosted runner pool.
- **Scale-to-fit**: aligns CI capacity with actual relevance of changes.

Sources:
- https://docs.github.com/en/actions/using-jobs/using-concurrency
- https://docs.gitlab.com/ee/ci/yaml/#interruptible
- https://learn.microsoft.com/en-us/azure/devops/pipelines/process/conditions

Measurement:

| Indicator | Method | Order of magnitude |
| --- | --- | --- |
| CI minutes / month | platform billing | -20 % to -50 % |

### **Severity / Remediation Cost**

Severity: **Info** — pattern-based recommendation.

Remediation cost: **Medium** — requires curating path lists.

### **Implementation principle**

- Target tech: YAML workflows.
- Detection mechanism: custom Sensor.
- Algorithm:

  ```
  for each workflow file:
      if job duration heuristic = "long" (>= 5 min based on previous runs OR declares matrix / docker build):
          checks:
            - declares paths or paths-ignore → otherwise report
            - declares concurrency with cancel-in-progress → otherwise report
            - PR-triggered job skips draft PRs → otherwise report (Info)
  ```

- Known false positives: workflows that legitimately must run for every commit (release, scheduled scans) — opt-out via comment.
- Feasibility verdict: ⚠️ (custom Sensor).
- Alternative tooling: actionlint, gh-action-stats.
