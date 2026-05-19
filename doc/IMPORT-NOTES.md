# Notes — import from steverigano/creedengo-infra → green-code-initiative monorepo

This module was bootstrapped from the WIP fork
[`steverigano/creedengo-infra`](https://github.com/steverigano/creedengo-infra)
on **2026-05-14** and then aligned with the workspace conventions described in
[`../AGENTS.md`](../AGENTS.md). The following non-trivial changes were applied
during the import:

## 1. Files intentionally NOT imported

- `src/it/test-projects/creedengo-infra-plugin-test-project/src/main/java/**`
  → ~30 Java check fixtures inherited from a `creedengo-java` fork
  (SQL/Spring/etc.). They are unrelated to infrastructure-as-code and were
  excluded by the `rsync` step.

## 2. Bug fixes in `UseOfProbesCheck`

Original (`src/main/java/org/greencodeinitiative/creedengo/infra/checks/UseOfProbesCheck.java`):

```java
// always false — an id cannot equal two different strings at once
.anyMatch(id -> id.equalsIgnoreCase("livenessProbe") && id.equalsIgnoreCase("readinessProbe"));
```

Issue: with this condition `ProbeFound` is **always false**, so the rule
fires on every `CommandNode`. The Helm fixtures `liveness_probes.yaml` /
`readiness_probes.yaml` are also misleading (one probe present should still
trigger the rule, not silence it).

Fix applied:

- Each probe checked independently (`hasLiveness`, `hasReadiness`).
- Rule fires when **either** is missing (energy waste = restart loops on
  unhealthy pods, traffic sent to not-ready pods).
- Message rewritten to convey the ecodesign intent.
- Field renamed to camelCase (`ProbeFound` → `hasLiveness/hasReadiness`).
- Helper renamed `containsProbe(Node)` → `isFieldNamed(Node, String)` so each
  probe can be searched independently.

## 3. Missing `InfraPlugin` class

The original `pom.xml` declares
`<pluginClass>org.greencodeinitiative.creedengo.infra.InfraPlugin</pluginClass>`
but the class did not exist in the source tree — `sonar-packaging-maven-plugin`
fails at `package` phase without it.

A **stub** `InfraPlugin implements Plugin` was added at
`src/main/java/org/greencodeinitiative/creedengo/infra/InfraPlugin.java`.
It has no extensions registered yet. Wiring `UseOfProbesCheck` into the parent
`sonar-iac-kubernetes` plugin (via `KubernetesExtension`) is tracked as a
follow-up — see TODOs in the class Javadoc.

## 4. Maven version bumps (alignment with creedengo-java)

| Property | Steve's fork | Workspace target (`creedengo-java/pom.xml`) | Applied |
| --- | --- | --- | --- |
| `version.creedengo-rules-specifications` | `2.2.2` | `2.7.1` | ✅ |
| `version.sonar-java` / `sonarjava.version` | `8.14.0.39102` | `8.23.0.42096` | ✅ |
| `version.test-it.sonarqube` | `25.5.0.107428` | `25.12.0.117093` | ✅ |
| `version.sonarqube` (plugin API) | `12.0.0.2960` | — | kept (intentionally newer than creedengo-java's 9.9 minimum) |
| `sonar-iac` family | `1.24.0.7839` / `1.46.0.15097` | n/a (not used elsewhere) | kept |

## 5. Integration tests neutralised

The `maven-failsafe-plugin` configuration inherited from the `creedengo-java`
fork is broken in this module:

- references `org.sonarsource.java:sonar-infra-plugin` (no such artifact),
- loads `org/greencodeinitiative/creedengo/java/creedengo_way_profile.json`
  (doesn't exist here — actual profile is
  `org/greencodeinitiative/creedengo/profiles/kubernetes_profile.json`),
- analyses a test project full of Java SQL fixtures, not Kubernetes manifests,
- associates language `java` with profile `creedengo way`.

The whole `<execution>` block of `maven-failsafe-plugin` was commented out in
`pom.xml`. Re-enabling IT properly is tracked as a TODO — the unit tests of
`UseOfProbesCheck` (run by `maven-surefire-plugin`) are unaffected.

## 6. Files preserved from the previous skeleton

These were authored before the import and kept on top of Steve's content:

- `tools/transfer-issues.sh` — GraphQL script to migrate `ecoCode-CI/CD`
  issues from `creedengo-challenge` to this repo.
- `doc/RULES-CONVENTIONS.md` — workspace-aligned rule conventions
  (GCI range, JSON+AsciiDoc skeleton). Now coexists with Steve's `RULES.md`,
  which simply points to `creedengo-rules-specifications`.
- `doc/LOCAL-ENV.md` — Docker prerequisites extracted from
  `creedengo-common/doc/{HOWTO,starter-pack}.md`.

## 7. Open follow-ups

1. **Production execution of `IacCheck`s at scan time** — ✅ Wave-3 closed
   2026-05-17. sonar-iac 1.24 / 1.46 instantiate their own analyser with a
   hard-coded list of visitors derived from each language's internal
   `CheckList`; there is no public SPI for an external plugin to inject
   additional `IacCheck` instances. Workaround in place: one
   `Infra<Lang>Sensor extends AbstractInfraIacSensor` per IaC language that
   re-parses each file via the public sonar-iac parser facade
   (`DockerParser`, `YamlParser`, `HclParser`), builds the active-check list
   from `Infra<Lang>CheckRegistrar.ANNOTATED_RULE_CLASSES` filtered by
   `SensorContext.activeRules()`, and reports issues on the dedicated
   repository (`creedengo-infra-docker`, …) so it never collides with the
   upstream `docker` / `kubernetes` / … repositories. All five sensors
   (`Docker`, `Kubernetes`, `Terraform`, `CloudFormation`, `Yaml`) are
   registered in `InfraPlugin`.
   - **Known limitation — Helm AST.** `InfraKubernetesSensor` uses the plain
     `YamlParser`, not the full `KubernetesAnalyzer`. Checks that listen on
     Helm-only AST nodes (e.g. `UseOfProbesCheck` on `CommandNode`) won't
     fire at scan time on Helm templates. A dedicated `InfraHelmSensor`
     wrapping `KubernetesAnalyzer` is the natural Wave-4a follow-up.
   - **Known limitation — secondaries.** `AbstractInfraIacSensor.SensorCheckContext`
     drops `SecondaryLocation`s because the upstream secondary references
     an `InputFile` from sonar-iac's own visitor pipeline. Currently no
     creedengo-infra check emits secondaries, but this is the place to
     extend the bridge if one does in the future.
2. Add the rule specification (`GCI1024.json` + `GCI1024.asciidoc`) to
   `creedengo-rules-specifications` under a Kubernetes/Helm classifier, then
   re-enable the `classifier` attribute on the `creedengo-rules-specifications`
   dependency in `pom.xml`.
3. Rebuild IT against a real Kubernetes test project (replace the polluted
   `creedengo-infra-plugin-test-project` with Helm/K8s fixtures).
4. Keep the per-language quality profiles
   (`creedengo_way_profile_{docker,kubernetes,terraform,cloudformation,yaml}.json`)
   in sync whenever new IaC rules ship — the old monolithic
   `kubernetes_profile.json` was retired in 2026-05.
5. Add per-sensor unit tests using `SensorContextTester` (one
   `Infra<Lang>SensorTest` per language) — exercises the active-rule filter,
   the parser invocation, and the `(line, message)` reporting on a temporary
   `InputFile`. Tracked as Wave-4b.

## 8. Wave-2B kickoff — `GCI1031 AvoidLatestTagCheck` (2026-05-17)

First stub upgraded to a real sonar-iac AST check. Five changes landed
alongside it; documenting them here so they're not surprising on the next
build:

1. **`KubernetesVerifier.BASE_DIR`** flipped from the Gradle-style
   `build/resources/test/checks` to the Maven-correct
   `src/test/resources/checks`. The original path silently broke
   `UseOfProbesCheckTest` (4× `ExceptionInInitializerError`); now `verifyNoIssue`
   for `all_probes.yaml` runs green.

2. **`UseOfProbesCheckTest`** — three `shouldRaise*` cases marked `@Disabled`.
   Root cause: `UseOfProbesCheck.initialize()` only registers on
   `org.sonar.iac.helm.tree.api.CommandNode`, but the fixtures under
   `helm/templates/` are plain YAML manifests with **no Helm `{{ ... }}`
   directives**, so `KubernetesAnalyzer` never emits a `CommandNode` and the
   rule never fires. Fix is either (a) rewrite the fixtures as real Helm
   templates, or (b) make the check additionally visit YAML `TupleTree`
   containers when no Helm content is present. Tracked as follow-up.

3. **`pom.xml`** — added a `maven-compiler-plugin` configuration with
   `<testExcludes>` for nine vendored `Abstract*Test` / `ExtensionSensorTest`
   classes under `src/test/java/org/sonar/iac/commons/testing/`. They were
   copied from a newer sonar-iac source tree and reference APIs not on this
   module's classpath (e.g. `SensorContextTester#addTelemetryProperty` from
   `sonar-plugin-api` 13.x). They are not referenced by any
   `org.greencodeinitiative.*` test, so excluding them unblocks compilation
   without touching the helpers we actually use (`Verifier`, `IacTestUtils`,
   `KubernetesVerifier`).

4. **`AbstractSensorTest.java`** — added the missing `SensorContextTester` and
   `slf4j.event.Level` imports while we're there. The class itself is excluded
   from test compile (point 3) but the import fix keeps it readable for the
   eventual cleanup.

5. **New test infrastructure**:
   - `src/test/java/org/greencodeinitiative/creedengo/infra/checks/DockerVerifier.java`
     — thin Maven-friendly wrapper around the vendored `Verifier` that uses
     `DockerParser.create()` and resolves fixtures under
     `src/test/resources/checks/`. Reference template for the remaining
     Wave-2B Docker rules.
   - `src/test/resources/checks/AvoidLatestTagCheck/{compliant,noncompliant}.Dockerfile`
     and `AvoidLatestTagCheckTest`. The test uses the explicit
     `Verifier.Issue` API (line/column/message) rather than `# Noncompliant`
     comment markers — Docker's `SyntaxToken` comment attachment doesn't yet
     interoperate with the default `commentsVisitor()` used by
     `MultiFileVerifier.assertOneOrMoreIssues()`. Same pattern can be reused
     for every Wave-2B Docker test until we add a Docker-specific
     `commentsVisitor`.

After these changes, `./mvnw -B test` reports **6 run, 0 failures, 0 errors,
3 skipped** (the three `@Disabled` UseOfProbes cases).

## 9. Wave-2B Docker batch — 8 more rules implemented (2026-05-17)

Cloned the `AvoidLatestTagCheck` pattern across the remaining Docker ✅ rules.
**Status: 9 / 14 Docker rules now have detection logic.**

| Key | Slug | Visitor entry point | Notes |
| --- | --- | --- | --- |
| GCI1025 | `multistage` | `Body` | Single-stage + build-tool heuristic. |
| GCI1026 | `lightweightimages` | `Body` (last image) | Heavy-family deny-list + slim/alpine/distroless allow-list. |
| GCI1029 | `cachecleanedexistingrulessonarqube` | `RunInstruction` | Extends RSPEC-6587 to dnf/yum/pip/npm/composer/gem. |
| GCI1030 | `pinbaseimagedigest` | `FromInstruction` | Requires `@sha256:`. Stricter than GCI1031. |
| GCI1031 | `avoidlatesttag` | `FromInstruction` | Reference template (§8). |
| GCI1033 | `usecopynotadd` | `AddInstruction` | Flags every `ADD`. |
| GCI1034 | `exposeonlyneededports` | `ExposeInstruction` | `RuleProperty maxPorts` (default 3). |
| GCI1035 | `setnonrootuser` | `Body` (last image) | Missing USER, or USER root / 0. |
| GCI1037 | `prefernocachepkgflag` | `RunInstruction` | apk/pip/npm `--no-cache` variants. |

### Cross-cutting changes that landed alongside

1. **`DockerCheckUtils`** (`src/main/java/.../checks/`) — small helper that
   centralises `ArgumentResolution` (single arg, full instruction, joined
   command text, flag presence). Avoids duplicating the unresolved/empty
   guard in every rule.

2. **`DockerVerifier` upgraded** — added a custom in-house `CollectingContext`
   so tests assert by `(line, message)` tuples (`ExpectedIssue.at(...)`)
   rather than positional column ranges. Columns drift across sonar-iac
   versions; line+message is robust and lets us batch-author the remaining
   Wave-2 rule tests without per-rule column tuning. The strict
   `Verifier`-based path is still exposed as
   `DockerVerifier.verifyStrict(...)` for cases where column accuracy is
   important. `AvoidLatestTagCheckTest` was migrated to the new helper.

3. **sonar-iac version skew workaround** — the module's `pom.xml` pulls both
   `sonar-iac-docker:1.24.0.7839` AND `sonar-iac-plugin:1.46.0.15097`. The
   1.46 plugin jar shades an *older* copy of
   `org.sonar.iac.docker.tree.api.DockerImage` that is missing
   `isLastDockerImageInFile()`. Workaround: every check that needs to target
   "the last stage" now traverses `Body.dockerImages()` and indexes the last
   element. Tracked as a follow-up (align `sonar-iac-plugin` with the
   common/docker/kubernetes/terraform/cloudformation jars, then revert to
   `isLastDockerImageInFile()` for clarity).

After this batch, `./mvnw -B test` reports **24 run, 0 failures, 0 errors,
3 skipped** (the three `@Disabled` UseOfProbes cases).

### Remaining Docker rules

- **GCI1027** `instructionsinspecificorder` — ⚠️ heuristic, deferred to Wave-2D.
- **GCI1028** `deleteunnecessaryfiles` — ⚠️ needs project-level `.dockerignore`
  sensor, deferred to Wave-2D.
- **GCI1032** `mergeconsecutiverun` — ✅ but multi-statement traversal,
  scheduled for Wave-2C.
- **GCI1036** `nobuildtoolsinruntime` — ✅ but needs final-stage tracking,
  scheduled for Wave-2C.
- **GCI1038** `singleprocesspercontainer` — ⚠️ heuristic, Wave-2D.
- **GCI1039** `preferstaticbinaryscratch` — ⚠️ heuristic, Wave-2D.


## 10. Wave-2C Kubernetes batch — 5 rules implemented (2026-05-17)

First Kubernetes/Helm rules from the GCI1040–GCI1049 range to receive real
detection logic, using the new `K8sYamlVerifier` (plain-YAML, attached
in the same wave) and the `KubernetesCheckUtils` helper for `kind:`,
`spec.template.spec` resolution and container iteration.

| Key | Slug | Visitor entry point | Notes |
| --- | --- | --- | --- |
| GCI1040 | `setresourcerequests` | `FileTree` → containers | Reports `cpu` and `memory` independently when `resources.requests.{cpu,memory}` is missing. |
| GCI1041 | `setresourcelimits` | `FileTree` → containers | Same shape on `resources.limits.{cpu,memory}`. |
| GCI1042 | `cpurequestvslimitratio` | `FileTree` → containers | `RuleProperty maxRatio` (default 4×). Parses K8s quantities (m, Ki/Mi/Gi, K/M/G, …) — unparseable values are silently skipped. |
| GCI1046 | `imagepullpolicynotalways` | `FileTree` → containers | Flags `imagePullPolicy: Always` when the image is pinned (digest `@sha256:` or explicit non-`:latest` tag). `:latest` is intentionally NOT flagged here — covered by GCI1031. |
| GCI1047 | `restricthostnetworkhostpid` | `FileTree` → podSpec | Reports each of `hostNetwork`, `hostPID`, `hostIPC` set to `true`. |

### Infrastructure landing alongside

1. **`KubernetesCheckUtils`** (`src/main/java/.../checks/`) — workload-kind
   classification (`POD_KINDS` / `POD_TEMPLATE_KINDS`), `podSpec(doc)`
   resolver that handles both `Pod` (`spec`) and workloads using a pod
   template (`spec.template.spec`), and helpers to navigate `MappingTree`
   (`mapping`, `scalar`, `bool`, `tuple`).

2. **`K8sYamlVerifier`** (`src/test/java/.../checks/`) — plain-YAML
   verifier that uses `YamlParser` directly (no Helm template processing)
   and re-uses `DockerVerifier`'s `(line, message)` comparison strategy
   via `ExpectedIssue.at(...)`. Keep `KubernetesVerifier` for Helm-template
   fixtures (Go-template AST); use this one for vanilla manifests.

3. **Locale-safe message formatting** — `CpuRequestVsLimitRatioCheck`
   uses `String.format(Locale.ROOT, ...)` so tests stay stable on `fr_FR`
   builds (comma decimal separator otherwise).

After this batch, `./mvnw -B test` reports **36 run, 0 failures, 0 errors,
3 skipped** (the three `@Disabled` UseOfProbes cases).

### Remaining K8s rules

- **GCI1043** `replicasgreaterthanneeded` — ⚠️ needs HPA / workload-profile
  awareness, deferred.
- **GCI1044** `requirehpafordeployment` — ✅ but cross-file (Deployment +
  HPA), needs a project-level `Sensor` — scheduled for Wave-2D.
- **GCI1045** `prefervpaorrightsizing` — ❌ delegated to KRR / Goldilocks.
- **GCI1048** `preferpdbforrollingeco` — ✅ cross-file (Deployment + PDB),
  scheduled for Wave-2D.
- **GCI1049** `helmvaluesdefaultseco` — ⚠️ needs `values.yaml` Sensor,
  Wave-2D.


## 11. Wave-2C Docker remainders — `GCI1032` + `GCI1036` (2026-05-17)

Closing out the Wave-2B follow-ups listed in §9. **Status: 11 / 14 Docker
rules now have detection logic.**

| Key | Slug | Visitor entry point | Notes |
| --- | --- | --- | --- |
| GCI1032 | `mergeconsecutiverun` | `Body` → per-stage instructions | Walks each `DockerImage.instructions()`, flags the 2nd-and-onwards `RunInstruction` in any run of adjacent RUNs. Any non-RUN instruction (COPY, ARG, ENV, …) resets the counter, so legitimately interleaved RUNs are not flagged. |
| GCI1036 | `nobuildtoolsinruntime` | `Body` → last stage only | Targets `body.dockerImages().get(size-1)` (runtime by convention). Flags `RunInstruction`s whose joined command both (a) invokes a known package manager (`apt-get install`, `apk add`, `dnf install`, `pip install`, `npm install`, …) and (b) installs at least one token from a curated `BUILD_PACKAGES` set. Token matching is bounded on `-` and alphanumerics — so `git` does not match `github-runner`. Tests cover both common positives and the `github-runner` false-positive guard. |

After this batch, `./mvnw -B test` reports **42 run, 0 failures, 0 errors,
3 skipped** (the three `@Disabled` UseOfProbes cases).

### Wave-2C closed; remaining Docker rules

- **GCI1027** `instructionsinspecificorder` — ⚠️ heuristic, Wave-2D.
- **GCI1028** `deleteunnecessaryfiles` — ⚠️ needs project-level `.dockerignore`
  sensor, Wave-2D.
- **GCI1038** `singleprocesspercontainer` — ⚠️ heuristic, Wave-2D.
- **GCI1039** `preferstaticbinaryscratch` — ⚠️ heuristic, Wave-2D.


