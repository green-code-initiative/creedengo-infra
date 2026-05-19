_(Proposed rule for creedengo-infra — GCI1065.)_

### **Rule title**

Pin third-party CI actions to an immutable SHA

### **Rule key**

GCI1065

### **Language and platform**

GitHub Actions / GitLab CI / Azure DevOps (YAML workflows)

### **Rule description**

External actions referenced by a moving ref (`@main`, `@master`, `@v4`, `@release`) silently change behaviour between runs. Two consequences:

1. **Energy waste**: every new upstream release invalidates the toolchain cache pulled by that action — the runner re-downloads dependencies, re-warms layer caches, and re-executes setup steps that would otherwise be a no-op.
2. **Supply-chain risk**: a compromised tag (see `tj-actions/changed-files` 2025) becomes an RCE the next time a workflow runs.

This rule reports any `uses:` step whose ref is **not** a 40-character lowercase hex Git SHA, except when the referenced action is:

* a first-party `actions/*` action (curated by GitHub),
* a locally-checked-out action (`./.github/actions/...`),
* a container action (`docker://...`),
* explicitly opted out by placing `eco-design:pin=tag` inside the step's `name:` or `if:` value
  (YAML strips `#`-comments before the AST, so the marker has to live in a real string field;
  as a last-resort escape hatch, it can also be appended to the `uses:` value itself,
  e.g. `uses: vendor/x@v1#eco-design:pin=tag`).

_Noncompliant Code Example_

```yaml
jobs:
  build:
    runs-on: ubuntu-22.04
    steps:
      - uses: tj-actions/changed-files@v44       # mutable tag
      - uses: some-org/deploy@main               # branch ref
```

_Compliant Solution_

```yaml
jobs:
  build:
    runs-on: ubuntu-22.04
    steps:
      - uses: tj-actions/changed-files@2f7c5bfce28377bc069a65ba478de0a74aa0ca32  # v44.5.7
      - uses: actions/checkout@v4                # first-party, exempt
      - uses: ./.github/actions/local            # local, exempt
```

### **Rule short description**

Third-party `uses:` references should be pinned to a full Git commit SHA.

### **Rule justification**

**Why it matters**:

* Mutable tags re-trigger upstream-toolchain downloads, which directly bills the runner's wall-time energy budget.
* Pinning to a SHA also dramatically improves reproducibility: the same workflow input always exercises the same dependency tree.

**Eco-design rationale** (axes 2 · 4 · 5):

* **Buildtime energy** (axis 2): stable caches across reruns; no surprise re-warming of action sub-dependencies.
* **Memory/CPU efficiency** (axis 4): avoids the periodic full pull cycle (a popular `setup-*` action can be ~50 MB of node-modules).
* **Provisioning efficiency** (axis 5): renovate-bot upgrades become explicit, batched and review-able instead of silent.

Sources:

* https://github.blog/security/supply-chain-security/uncovering-a-supply-chain-attack-on-tj-actions-changed-files/
* https://docs.github.com/en/actions/security-guides/security-hardening-for-github-actions#using-third-party-actions
* https://securitylab.github.com/research/github-actions-untrusted-input/

Measurement:

| Indicator | Method | Order of magnitude |
| --- | --- | --- |
| Workflow rerun-to-rerun toolchain cache misses | CI logs (`Cache restored from key:`) | -X % after pinning |
| Mean time between unintended upstream upgrades | Renovate / Dependabot reports | ∞ (manual upgrades only) |

### **Severity / Remediation Cost**

Severity: **Minor**.

Remediation cost: **Easy** — replace the tag by the resolved SHA. Renovate/Dependabot can keep both the SHA and the tag-as-comment in sync.

### **Implementation principle**

* Target tech: YAML workflows (`.github/workflows/**/*.yml`, GitLab `.gitlab-ci.yml`).
* Detection mechanism: sonar-iac YAML AST (`MappingTree`) — same plumbing as the other CI rules (`CiYamlCheckUtils`).
* Algorithm:

  ```
  for each workflow.jobs[*].steps[*]:
      uses = step.uses
      if uses missing → skip                          (run-only step)
      if uses startsWith "./" or "docker://" → skip   (local / container)
      (owner, ref) = uses.split("@", 2)
      if owner startsWith "actions/" → skip           (first-party)
      if ref matches ^[0-9a-f]{40}$ → skip            (pinned)
      if step.comment contains "eco-design:pin=tag" → skip
      report("Pin '<uses>' to an immutable SHA40 instead of moving ref '<ref>'.")
  ```

* Known false positives:
  * GitHub Marketplace actions intentionally consumed by tag in throwaway sandbox workflows — addressed by the `eco-design:pin=tag` opt-out.
  * Internal reusable workflows (`uses: org/repo/.github/workflows/foo.yml@<ref>`) referencing a branch on the same monorepo — currently flagged on purpose; teams can opt out per step.
* Feasibility verdict: ⚠️ (heuristic — depends on the comment opt-out and the local/first-party allow-list).
* Alternative tooling: `actionlint --ignore-pattern`, `pinact`, GitHub Dependabot grouped updates.

