# creedengo-infra rules index

Authoritative metadata source: [`creedengo-rules-specifications`](../creedengo-rules-specifications/). This file is a local convenience index for the **infra** classifier (Docker, Kubernetes/Helm, Terraform, CloudFormation, IaC transverse).

📚 **Factual justification & literature review for every rule below** → [`doc/RULES-JUSTIFICATIONS.md`](./doc/RULES-JUSTIFICATIONS.md).

**Reserved  key range for `creedengo-infra`: `1020` → `1099`.**

## Status legend

| Symbol | Meaning                                                         |
|--------|-----------------------------------------------------------------|
| ✅      | Implementable with sonar-iac AST visitors                       |
| ⚠️     | Implementable with custom Sensor or heuristic (lower precision) |
| ❌      | Not implementable statically — delegated to an external tool    |

## Active rules

### Kubernetes (existing)

| Key  | Slug            | Severity | Remediation | Feasibility |
|------|-----------------|----------|-------------|-------------|
| 1024 | `use-of-probes` | Minor    | Easy        | ✅           |

### Docker — current drafts (1025–1029)

| Key | Slug | Severity | Remediation | Feasibility |
| --- | --- | --- | --- | --- |
| 1025 | [multistage](savespottedrules/multistage.md) **(impl)** | Major | Medium | ✅ |
| 1026 | [lightweightimages](savespottedrules/lightweightimages.md) **(impl)** | Major | Easy | ✅ |
| 1027 | [instructionsinspecificorder](savespottedrules/instructionsinspecificorder.md) **(impl)** | Minor | Easy | ⚠️ |
| 1028 | [deleteunnecessaryfiles](savespottedrules/deleteunnecessaryfiles.md) **(impl)** | Major | Easy | ⚠️ |
| 1029 | [cachecleanedexistingrulessonarqube](savespottedrules/cachecleanedexistingrulessonarqube.md) **(impl)** | Major | Easy | ✅ |

### Docker / OCI — new (1030–1039)

| Key | Slug | Severity | Remediation | Feasibility |
| --- | --- | --- | --- | --- |
| 1030 | [pinbaseimagedigest](savespottedrules/pinbaseimagedigest.md) **(impl)** | Minor | Easy | ✅ |
| 1031 | [avoidlatesttag](savespottedrules/avoidlatesttag.md) **(impl)** | Major | Easy | ✅ |
| 1032 | [mergeconsecutiverun](savespottedrules/mergeconsecutiverun.md) **(impl)** | Minor | Medium | ✅ |
| 1033 | [usecopynotadd](savespottedrules/usecopynotadd.md) **(impl)** | Minor | Easy | ✅ |
| 1034 | [exposeonlyneededports](savespottedrules/exposeonlyneededports.md) **(impl)** | Minor | Easy | ✅ |
| 1035 | [setnonrootuser](savespottedrules/setnonrootuser.md) **(impl)** | Major | Easy | ✅ |
| 1036 | [nobuildtoolsinruntime](savespottedrules/nobuildtoolsinruntime.md) **(impl)** | Major | Medium | ✅ |
| 1037 | [prefernocachepkgflag](savespottedrules/prefernocachepkgflag.md) **(impl)** | Minor | Easy | ✅ |
| 1038 | [singleprocesspercontainer](savespottedrules/singleprocesspercontainer.md) **(impl)** | Info | Hard | ⚠️ |
| 1039 | [preferstaticbinaryscratch](savespottedrules/preferstaticbinaryscratch.md) | Info | Hard | ⚠️ |

### Kubernetes / Helm — new (1040–1049)

| Key | Slug | Severity | Remediation | Feasibility |
| --- | --- | --- | --- | --- |
| 1040 | [setresourcerequests](savespottedrules/setresourcerequestsandlimits.md) **(impl)** | Major | Easy | ✅ |
| 1041 | [setresourcelimits](savespottedrules/setresourcelimits.md) **(impl)** | Major | Easy | ✅ |
| 1042 | [cpurequestvslimitratio](savespottedrules/cpurequestvslimitratio.md) **(impl)** | Minor | Medium | ✅ |
| 1043 | [replicasgreaterthanneeded](savespottedrules/replicasgreaterthanneeded.md) **(impl)** | Minor | Hard | ⚠️ |
| 1044 | [requirehpafordeployment](savespottedrules/requirehpafordeployment.md) **(impl)** | Minor | Medium | ✅ |
| 1045 | [prefervpaorrightsizing](savespottedrules/prefervpaorrightsizing.md) | Info | Hard | ❌ |
| 1046 | [imagepullpolicynotalways](savespottedrules/imagepullpolicynotalways.md) **(impl)** | Minor | Easy | ✅ |
| 1047 | [restricthostnetworkhostpid](savespottedrules/restricthostnetworkhostpid.md) **(impl)** | Info | Easy | ✅ |
| 1048 | [preferpdbforrollingeco](savespottedrules/preferpdbforrollingeco.md) **(impl)** | Info | Medium | ✅ |
| 1049 | [helmvaluesdefaultseco](savespottedrules/helmvaluesdefaultseco.md) **(impl)** | Info | Medium | ⚠️ |

### Terraform / CloudFormation — new (1050–1057)

| Key | Slug | Severity | Remediation | Feasibility |
| --- | --- | --- | --- | --- |
| 1050 | [tfprefergravitonarm](savespottedrules/tfprefergravitonarm.md) **(impl)** | Minor | Medium | ✅ |
| 1051 | [tfrightsizeinstancetypes](savespottedrules/tfrightsizeinstancetypes.md) **(impl)** | Info | Hard | ⚠️ |
| 1052 | [tfenableautoscalinggroup](savespottedrules/tfenableautoscalinggroup.md) **(impl)** | Major | Medium | ✅ |
| 1053 | [tfstoragelifecyclerules](savespottedrules/tfstoragelifecyclerules.md) **(impl)** | Minor | Easy | ✅ |
| 1054 | [tfchooselowcarbonregion](savespottedrules/tfchooselowcarbonregion.md) **(impl)** | Info | Easy | ✅ |
| 1055 | [tfavoidalwaysonresources](savespottedrules/tfavoidalwaysonresources.md) **(impl)** | Major | Medium | ⚠️ |
| 1056 | [tfpreferspotorsavings](savespottedrules/tfpreferspotorsavings.md) **(impl)** | Info | Hard | ⚠️ |
| 1057 | [cfnprefergravitonlowcarbon](savespottedrules/cfnprefergravitonlowcarbon.md) **(impl)** | Minor | Medium | ✅ |
| 1066 | [tfmultiregionandserverlesshint](tfmultiregionandserverlesshint.md) **(impl)** | Info | Easy | ✅ |

### IaC transverse — new (1058–1065)

| Key | Slug | Severity | Remediation | Feasibility |
| --- | --- | --- | --- | --- |
| 1058 | [cicachedependencies](savespottedrules/cicachedependencies.md) **(impl)** | Minor | Easy | ⚠️ |
| 1059 | [ciskipredundantjobs](savespottedrules/ciskipredundantjobs.md) **(impl)** | Info | Medium | ⚠️ |
| 1060 | [cipinrunnerandarm](savespottedrules/cipinrunnerandarm.md) **(impl)** | Info | Easy | ⚠️ |
| 1061 | [iacnosecretinplaintexteco](savespottedrules/iacnosecretinplaintexteco.md) **(impl)** | Major | Easy | ✅ |
| 1062 | [iactagenvironmenteco](savespottedrules/iactagenvironmenteco.md) **(impl)** | Info | Easy | ✅ |
| 1063 | [iacprometheuslowretentiondefault](savespottedrules/iacprometheuslowretentiondefault.md) **(impl)** | Info | Medium | ⚠️ |
| 1064 | [iacscheduledscaledownnonprod](savespottedrules/iacscheduledscaledownnonprod.md) **(impl)** | Major | Medium | ⚠️ |
| 1065 | [cipinactions](savespottedrules/cipinactions.md) **(impl)** | Minor | Easy | ⚠️ |

## Notes

- `multistagebuilddocker.md` was archived in 2026-05 (superseded by `multistage.md`).
- Quality profile is split per language: `creedengo_way_profile_docker.json`, `creedengo_way_profile_kubernetes.json`, `creedengo_way_profile_terraform.json`, `creedengo_way_profile_cloudformation.json`, `creedengo_way_profile_yaml.json`.
- **Wave-2B status (2026-05-17)** — `1031` (`AvoidLatestTagCheck`) is the first stub upgraded to a real sonar-iac AST check. It visits `FromInstruction`, resolves the image via `ArgumentResolution.of(...)`, and flags missing or `:latest` tags while honouring digest pins (`@sha256:`) and `FROM scratch`. It is the reference template for the remaining Docker rules in Wave-2B. See `doc/IMPORT-NOTES.md` §8 for the test-infra/Maven changes that landed alongside it.
- **Wave-2D status (2026-05-17)** — The remaining Terraform stubs (`1051`, `1055`, `1056`) and the IaC-transverse stubs (`1058`–`1064`) now ship real detection. Terraform checks reuse `TerraformCheckUtils`; CI / IaC-transverse YAML checks parse via the public `YamlParser` and the new `CiYamlCheckUtils` helper. The `InfraYamlSensorTest` now asserts a real issue is raised (via `1060` on `runs-on: ubuntu-latest`) instead of the previous stub smoke-test.
