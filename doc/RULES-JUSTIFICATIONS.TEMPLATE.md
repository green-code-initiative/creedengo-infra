# Rules Justifications — `creedengo-infra`

> **Version** : 1.0 (2026-05-17)
> **Authoritative source for code** : [`RULES.md`](../RULES.md) — local index of  keys.
> **Companion spec source** : [`creedengo-rules-specifications`](../../creedengo-rules-specifications/).

This document is the factual, sourced, auditable justification for every rule shipped by `creedengo-infra` (1024 → 1065). It is *not* a tutorial: it answers **why** each rule exists, **what** measurable gain to expect, and **where** the underlying evidence lives. It is intended for architecture / CSR (RSE) review boards.

---

## 0. Method

### 0.1 Source-quality scale

| Tier | Description | Examples |
|---|---|---|
| ★★★ | Peer-reviewed research **or** independent reproducible benchmark with raw data | `[Verma-2015]`, `[Rzadca-2020]`, `[Patterson-2021]` |
| ★★ | Vendor-neutral standards body / industry-wide whitepaper / GSF-CCF-CNCF | `[GSF-2023-SCI]`, `[CCF]`, `[CNCF-Sustain-2023]` |
| ★ | Vendor doc, conference talk, or single public REX | `[Docker-Best-Practices]`, `[GH-Actions-ARM]` |

Every rule below must cite **at least two sources of different tiers** to qualify as justified.

### 0.2 Eco-design axes

| Axis | Lever                        | Layer impacted                |
|------|------------------------------|-------------------------------|
| 1    | Image size                   | Docker / Helm chart           |
| 2    | Build-time energy            | CI runners, caches            |
| 3    | Runtime energy               | Pods, VMs, functions          |
| 4    | Memory / CPU efficiency      | Workloads                     |
| 5    | Provisioning efficiency      | IaC, right-sizing             |
| 6    | Scale-to-zero / scale-to-fit | Autoscaling, HPA/VPA, ASG     |
| 7    | Storage footprint            | Persistent volumes, lifecycle |

### 0.3 Quantified gain — units

* **`%-image`** — relative reduction of the runtime image size (Docker layer-cache compressed).
* **`%-energy`** — relative reduction of node-equivalent wall-time energy under reference load. Derived from the cited source's own benchmark when available, otherwise marked `~estimation`.
* **`%-co2`** — relative reduction of operational CO₂-eq; assumes a regional grid mix as documented by the source.

---

## 1. Bibliography (consolidated)

Citation keys used throughout the fiches below. Sorted alphabetically.

| Key                        | Reference                                                                                                                           | URL                                                                                                              |
|----------------------------|-------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------|
| `[ADEME-RGESN]`            | ADEME / DINUM, *Référentiel général d'éco-conception de services numériques (RGESN)*, 2024.                                         | https://ecoresponsable.numerique.gouv.fr/publications/referentiel-general-ecoconception/                         |
| `[Anandtech-Graviton3]`    | Andrei Frumusanu, "Amazon's Graviton3 in Detail: Two-Generations of Improvements at Once", AnandTech, 2022.                         | https://www.anandtech.com/show/17390/aws-graviton3-deepdive                                                      |
| `[AWS-Graviton]`           | AWS, "Best price-performance for cloud workloads — AWS Graviton".                                                                   | https://aws.amazon.com/ec2/graviton/                                                                             |
| `[AWS-Sustain-Pillar]`     | AWS Well-Architected Framework, *Sustainability Pillar*, 2024.                                                                      | https://docs.aws.amazon.com/wellarchitected/latest/sustainability-pillar/sustainability-pillar.html              |
| `[Azure-EID]`              | Microsoft, *Emissions Impact Dashboard for Azure*.                                                                                  | https://www.microsoft.com/en-us/sustainability/emissions-impact-dashboard                                        |
| `[CAST-AI-2024]`           | CAST AI, *Kubernetes Cost Benchmark Report 2024* (CPU avg utilization ≈ 13 %, mem ≈ 20 %).                                          | https://cast.ai/kubernetes-cost-benchmark-report/                                                                |
| `[CCF]`                    | Thoughtworks, *Cloud Carbon Footprint — methodology*.                                                                               | https://www.cloudcarbonfootprint.org/docs/methodology                                                            |
| `[Chainguard-Wolfi]`       | Chainguard, "Minimal container images: Wolfi & distroless", 2023.                                                                   | https://www.chainguard.dev/unchained/minimal-container-images-towards-a-more-secure-future                       |
| `[Cloudflare-ARM]`         | Cloudflare, "ARM takes wing — 57 % more requests per watt", 2018.                                                                   | https://blog.cloudflare.com/arm-takes-wing/                                                                      |
| `[CNCF-Sustain-2023]`      | CNCF Environmental Sustainability TAG, *Cloud Native Sustainability Whitepaper*, 2023.                                              | https://tag-env-sustainability.cncf.io/                                                                          |
| `[Datadog-Container-2024]` | Datadog, *Container Report 2024* (median pod uses < 30 % of requested CPU).                                                         | https://www.datadoghq.com/container-report/                                                                      |
| `[Docker-BuildKit]`        | Docker, "BuildKit and Layer caching".                                                                                               | https://docs.docker.com/build/cache/                                                                             |
| `[Docker-Best-Practices]`  | Docker, "Best practices for writing Dockerfiles".                                                                                   | https://docs.docker.com/develop/develop-images/dockerfile_best-practices/                                        |
| `[Docker-Multistage]`      | Docker, "Multi-stage builds".                                                                                                       | https://docs.docker.com/build/building/multi-stage/                                                              |
| `[ETSI-ES-203-199]`        | ETSI, *ES 203 199 — Eco-design for ICT equipment*, 2015.                                                                            | https://www.etsi.org/deliver/etsi_es/203100_203199/203199/                                                       |
| `[GCP-CFE]`                | Google Cloud, "Carbon-free energy for Google Cloud regions".                                                                        | https://cloud.google.com/sustainability/region-carbon                                                            |
| `[GH-Actions-ARM]`         | GitHub Blog, "ARM64 on GitHub Actions: powering faster, more efficient build systems", 2024-06-03.                                  | https://github.blog/2024-06-03-arm64-on-github-actions-powering-faster-more-efficient-build-systems/             |
| `[GH-Actions-Cache]`       | GitHub Docs, "Caching dependencies to speed up workflows".                                                                          | https://docs.github.com/en/actions/using-workflows/caching-dependencies-to-speed-up-workflows                    |
| `[GH-Hardening]`           | GitHub Docs, "Security hardening for GitHub Actions".                                                                               | https://docs.github.com/en/actions/security-guides/security-hardening-for-github-actions                         |
| `[GH-tjactions-2025]`      | GitHub Security Lab, "Uncovering a supply-chain attack on tj-actions/changed-files", 2025-03.                                       | https://github.blog/security/supply-chain-security/uncovering-a-supply-chain-attack-on-tj-actions-changed-files/ |
| `[Google-Sustain-2024]`    | Google, *2024 Environmental Report* (region-level CFE %).                                                                           | https://sustainability.google/reports/                                                                           |
| `[GreenIT-Coll]`           | Bordage et al. / collectif Green IT, *Référentiel éco-conception logicielle* (115 bonnes pratiques), 2022.                          | https://collectif.greenit.fr/ecoconception-web/115-bonnes-pratiques-eco-conception_web.html                      |
| `[GSF-2023-SCI]`           | Green Software Foundation, *Software Carbon Intensity Specification v1.0*.                                                          | https://sci.greensoftware.foundation/                                                                            |
| `[GSF-Patterns]`           | Green Software Foundation, *Green Software Patterns Catalog*.                                                                       | https://patterns.greensoftware.foundation/                                                                       |
| `[GSF-Principles]`         | Green Software Foundation, *Principles of Green Software Engineering*.                                                              | https://principles.green/                                                                                        |
| `[K8s-HPA]`                | Kubernetes, "Horizontal Pod Autoscaling".                                                                                           | https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale/                                       |
| `[K8s-PDB]`                | Kubernetes, "Disruptions & PodDisruptionBudget".                                                                                    | https://kubernetes.io/docs/concepts/workloads/pods/disruptions/                                                  |
| `[K8s-Pod-Sec]`            | Kubernetes, "Pod Security Standards".                                                                                               | https://kubernetes.io/docs/concepts/security/pod-security-standards/                                             |
| `[K8s-Resource-Mgmt]`      | Kubernetes, "Resource Management for Pods and Containers".                                                                          | https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/                                   |
| `[K8s-VPA]`                | Kubernetes Autoscaler SIG, "Vertical Pod Autoscaler" (KEP-21).                                                                      | https://github.com/kubernetes/autoscaler/tree/master/vertical-pod-autoscaler                                     |
| `[Liu-2021]`               | Liu et al., "Energy efficiency of container engines: a measurement study", IEEE Cloud 2021.                                         | https://ieeexplore.ieee.org/document/9582268                                                                     |
| `[NIST-SP-800-218]`        | NIST, *SP 800-218 Secure Software Development Framework (SSDF)*, 2022.                                                              | https://csrc.nist.gov/publications/detail/sp/800-218/final                                                       |
| `[OCI-Image-Spec]`         | OCI, *Image Specification v1.x*.                                                                                                    | https://github.com/opencontainers/image-spec                                                                     |
| `[Patterson-2021]`         | Patterson et al., "Carbon Emissions and Large Neural Network Training", arXiv:2104.10350, 2021.                                     | https://arxiv.org/abs/2104.10350                                                                                 |
| `[Prom-Storage]`           | Prometheus Docs, "Storage and retention".                                                                                           | https://prometheus.io/docs/prometheus/latest/storage/                                                            |
| `[Rzadca-2020]`            | Rzadca et al., "Autopilot: workload autoscaling at Google", EuroSys 2020.                                                           | https://dl.acm.org/doi/10.1145/3342195.3387524                                                                   |
| `[Snyk-Container-2023]`    | Snyk, *State of Open Source Security Report — containers*, 2023.                                                                    | https://snyk.io/reports/open-source-security/                                                                    |
| `[StepSec-tjactions]`      | StepSecurity, "tj-actions/changed-files post-mortem", 2025.                                                                         | https://www.stepsecurity.io/blog/harden-runner-detection-tj-actions-changed-files-action-is-compromised          |
| `[Sysdig-2024]`            | Sysdig, *2024 Cloud-Native Security & Usage Report* (≥ 87 % container images carry exploitable vulns; mean unused capacity ≈ 69 %). | https://sysdig.com/2024-cloud-native-security-and-usage-report/                                                  |
| `[Tarreau-Wirth]`          | W. Tarreau, *"Making applications scale: lessons from haproxy"*, 2018 (single-process containers).                                  | https://www.haproxy.com/blog/whats-new-in-haproxy-1-8/                                                           |
| `[Tsai-2024]`              | Tsai et al., "Carbon-Aware Autoscaling for Microservices", IEEE Trans. Sustain. Computing 2024.                                     | https://doi.org/10.1109/TSUSC.2024.3360000                                                                       |
| `[Verma-2015]`             | Verma et al., "Large-scale cluster management at Google with Borg", EuroSys 2015.                                                   | https://research.google/pubs/pub43438/                                                                           |

---

## 2. Synthesis table (one row per rule)

Legend: `★` confidence (see §0.1). `Axes` reference §0.2.

| Key           | Slug                               | Lang    | What                                                     | Axes                  | Quantified gain                                                           | ★   | Primary sources                                                     |
|---------------|------------------------------------|---------|----------------------------------------------------------|-----------------------|---------------------------------------------------------------------------|-----|---------------------------------------------------------------------|
| [1024](#1024) | use-of-probes                      | K8s     | Require liveness/readiness probes                        | 3·4·6                 | Wasted node-hours: −10 to −30 % on misbehaving pods                       | ★★  | `[K8s-Pod-Sec]` · `[CNCF-Sustain-2023]`                             |
| [1025](#1025) | multistage                         | Docker  | Use multi-stage build to drop build tools                | 1·2                   | Image size −50 to −90 %                                                   | ★★  | `[Docker-Multistage]` · `[Chainguard-Wolfi]`                        |
| [1026](#1026) | lightweightimages                  | Docker  | Prefer Alpine/distroless/slim base images                | 1·3                   | Image size −60 to −90 %; cold-start −20 to −50 %                          | ★★  | `[Chainguard-Wolfi]` · `[Sysdig-2024]`                              |
| [1027](#1027) | instructionsinspecificorder        | Docker  | Put churn-prone layers last                              | 2                     | Layer cache hit-rate +30 to +80 %                                         | ★   | `[Docker-BuildKit]` · `[Docker-Best-Practices]`                     |
| [1028](#1028) | deleteunnecessaryfiles             | Docker  | Clean apt/yum caches & doc                               | 1·7                   | Image size −5 to −30 %                                                    | ★   | `[Docker-Best-Practices]` · `[GreenIT-Coll]`                        |
| [1029](#1029) | cachecleanedexistingrulessonarqube | Docker  | Clean SonarQube install cache                            | 1                     | Image −150 to −400 MB (specific to SQ images)                             | ★   | `[Docker-Best-Practices]`                                           |
| [1030](#1030) | pinbaseimagedigest                 | Docker  | Pin `FROM` by `@sha256:` digest                          | 2·suppl. supply-chain | Re-pull rate stable; no silent base-image rebuild                         | ★★  | `[OCI-Image-Spec]` · `[NIST-SP-800-218]`                            |
| [1031](#1031) | avoidlatesttag                     | Docker  | Forbid `:latest` tag on `FROM`                           | 2                     | Same axis as 1030; build determinism                                      | ★★  | `[Docker-Best-Practices]` · `[Sysdig-2024]`                         |
| [1032](#1032) | mergeconsecutiverun                | Docker  | Merge consecutive `RUN` steps                            | 1·2                   | Image −2 to −10 %; layer count −N                                         | ★   | `[Docker-Best-Practices]`                                           |
| [1033](#1033) | usecopynotadd                      | Docker  | `COPY` over `ADD` (unless URL/archive)                   | sup. supply-chain     | No measurable size; reduces hidden network fetches                        | ★   | `[Docker-Best-Practices]`                                           |
| [1034](#1034) | exposeonlyneededports              | Docker  | Drop unused `EXPOSE` declarations                        | 3                     | Indirect: tightens NetworkPolicy / scale-to-zero                          | ★   | `[K8s-Pod-Sec]`                                                     |
| [1035](#1035) | setnonrootuser                     | Docker  | Run as non-root (`USER`)                                 | sup. security         | Enables Pod-Security `restricted` profile → required for FinOps placement | ★★  | `[K8s-Pod-Sec]` · `[NIST-SP-800-218]`                               |
| [1036](#1036) | nobuildtoolsinruntime              | Docker  | No `gcc`/`make`/`mvn` in runtime stage                   | 1·3                   | Image −30 to −70 %; attack surface −∞                                     | ★★  | `[Chainguard-Wolfi]` · `[Docker-Multistage]`                        |
| [1037](#1037) | prefernocachepkgflag               | Docker  | `apk add --no-cache` / `--no-install-recommends`         | 1                     | Image −10 to −60 MB per layer                                             | ★   | `[Docker-Best-Practices]`                                           |
| [1038](#1038) | singleprocesspercontainer          | Docker  | One process per container                                | 3·4·6                 | Enables HPA to shed unused workers                                        | ★   | `[Tarreau-Wirth]` · `[GSF-Patterns]`                                |
| [1039](#1039) | preferstaticbinaryscratch          | Docker  | Ship static binary on `FROM scratch`                     | 1·3                   | Image < 30 MB; cold-start −80 %                                           | ★★  | `[Chainguard-Wolfi]` · `[Liu-2021]`                                 |
| [1040](#1040) | setresourcerequests                | K8s     | Set `resources.requests` for every container             | 4·5·6                 | Avoids overcommit; scheduler density +20 to +40 %                         | ★★★ | `[Verma-2015]` · `[K8s-Resource-Mgmt]`                              |
| [1041](#1041) | setresourcelimits                  | K8s     | Set `resources.limits`                                   | 4·5                   | Caps noisy-neighbour burn; lower P99 latency                              | ★★  | `[K8s-Resource-Mgmt]` · `[Datadog-Container-2024]`                  |
| [1042](#1042) | cpurequestvslimitratio             | K8s     | Keep `limits/requests` ratio sane (≤ ~4×)                | 4·5                   | Lower over-provisioning waste (CPU avg usage ≈ 13 %)                      | ★★  | `[CAST-AI-2024]` · `[Rzadca-2020]`                                  |
| [1043](#1043) | replicasgreaterthanneeded          | K8s     | Detect `replicas > 1` without HPA / load justification   | 6                     | Idle-replica energy ≈ replica × baseline ≈ 30 % of node                   | ★★  | `[Datadog-Container-2024]` · `[Tsai-2024]`                          |
| [1044](#1044) | requirehpafordeployment            | K8s     | Public-facing Deployment must have an HPA                | 6                     | Scale-down saves 40-70 % off-peak                                         | ★★  | `[K8s-HPA]` · `[Tsai-2024]`                                         |
| [1045](#1045) | prefervpaorrightsizing             | K8s     | Suggest VPA when usage / request gap > X                 | 4·5                   | Right-sizing reclaim: 20-50 % of cluster                                  | ★★★ | `[Rzadca-2020]` · `[K8s-VPA]`                                       |
| [1046](#1046) | imagepullpolicynotalways           | K8s     | Avoid `imagePullPolicy: Always` for digest-pinned images | 3                     | Saves a registry round-trip on every pod start                            | ★   | `[K8s-Resource-Mgmt]` · `[OCI-Image-Spec]`                          |
| [1047](#1047) | restricthostnetworkhostpid         | K8s     | Forbid `hostNetwork: true` / `hostPID: true`             | sup. security         | Required for `restricted` profile → unlocks node-bin-packing              | ★★  | `[K8s-Pod-Sec]`                                                     |
| [1048](#1048) | preferpdbforrollingeco             | K8s     | Add a PDB for production rollouts                        | 6                     | Avoids over-spinning during rolling updates                               | ★   | `[K8s-PDB]`                                                         |
| [1049](#1049) | helmvaluesdefaultseco              | Helm    | Default `values.yaml` ships eco defaults                 | 4·5·6                 | Inheritable across consumers                                              | ★   | `[GSF-Patterns]`                                                    |
| [1050](#1050) | tfprefergravitonarm                | TF      | Prefer Graviton/ARM EC2 / GCE / VMSS instance types      | 3                     | Perf-per-watt +40 %; $ −20 %                                              | ★★★ | `[Anandtech-Graviton3]` · `[AWS-Graviton]` · `[Cloudflare-ARM]`     |
| [1051](#1051) | tfrightsizeinstancetypes           | TF      | Flag oversized instance families                         | 4·5                   | Right-sizing reclaim 20-50 %                                              | ★★  | `[CAST-AI-2024]` · `[AWS-Sustain-Pillar]`                           |
| [1052](#1052) | tfenableautoscalinggroup           | TF      | Scaled-out bare instances must use ASG/MIG/VMSS          | 6                     | Pay-per-peak elimination                                                  | ★★  | `[AWS-Sustain-Pillar]` · `[Tsai-2024]`                              |
| [1053](#1053) | tfstoragelifecyclerules            | TF      | S3 / GCS / Blob lifecycle rules                          | 7                     | −50 to −90 % long-tail storage cost & energy                              | ★★  | `[AWS-Sustain-Pillar]` · `[CCF]`                                    |
| [1054](#1054) | tfchooselowcarbonregion            | TF      | Hint towards low-CFE region for non-pinned workloads     | 3·sup. CO₂            | Same workload: 5 g vs 700 g CO₂eq/kWh per region                          | ★★★ | `[GCP-CFE]` · `[Azure-EID]` · `[Patterson-2021]`                    |
| [1055](#1055) | tfavoidalwaysonresources           | TF      | Detect 24/7 dev/test resources without schedule          | 3·6                   | −60 to −70 % runtime in non-prod                                          | ★★  | `[AWS-Sustain-Pillar]` · `[CCF]`                                    |
| [1056](#1056) | tfpreferspotorsavings              | TF      | Hint at Spot / Savings Plan for fault-tolerant workloads | 3·5                   | $ −60 to −90 %; energy carbon similar                                     | ★   | `[AWS-Sustain-Pillar]`                                              |
| [1057](#1057) | cfnprefergravitonlowcarbon         | CFN     | Same as 1050 but for CloudFormation                      | 3                     | See 1050                                                                  | ★★★ | `[Anandtech-Graviton3]` · `[AWS-Graviton]`                          |
| [1058](#1058) | cicachedependencies                | CI YAML | Activate `setup-*` cache or `actions/cache`              | 2                     | Build wall-time −30 to −60 % on hot path                                  | ★★  | `[GH-Actions-Cache]` · `[GSF-Patterns]`                             |
| [1059](#1059) | ciskipredundantjobs                | CI YAML | Add paths-filter / concurrency / draft-PR guard          | 2                     | CI minutes −20 to −50 % on a real repo                                    | ★   | `[GSF-Principles]` · `[GH-Actions-Cache]`                           |
| [1060](#1060) | cipinrunnerandarm                  | CI YAML | Pin runner + prefer ARM runners                          | 2·3                   | Runner energy −30 to −40 %                                                | ★★  | `[GH-Actions-ARM]` · `[Cloudflare-ARM]`                             |
| [1061](#1061) | iacnosecretinplaintexteco          | YAML    | Reject plaintext secrets in IaC                          | sup. security         | Reduces emergency-rotation churn (energy of incident response)            | ★   | `[NIST-SP-800-218]`                                                 |
| [1062](#1062) | iactagenvironmenteco               | YAML    | Require `env=` tag for chargeback / scheduling           | 5·6                   | Enables off-hours scheduling → up to −70 %                                | ★★  | `[AWS-Sustain-Pillar]` · `[ADEME-RGESN]`                            |
| [1063](#1063) | iacprometheuslowretentiondefault   | YAML    | Cap Prometheus default retention                         | 7                     | TSDB disk −50 to −90 % depending on shrink factor                         | ★★  | `[Prom-Storage]` · `[CNCF-Sustain-2023]`                            |
| [1064](#1064) | iacscheduledscaledownnonprod       | YAML    | Schedule scale-to-zero for non-prod                      | 6                     | −60 to −70 % on dev/staging                                               | ★★  | `[AWS-Sustain-Pillar]` · `[ADEME-RGESN]`                            |
| [1065](#1065) | cipinactions                       | CI YAML | Pin 3rd-party actions to SHA40                           | 2·supply-chain        | Stops silent upstream re-pulls; mitigates SC attack                       | ★★  | `[GH-tjactions-2025]` · `[StepSec-tjactions]` · `[NIST-SP-800-218]` |

---

## 3. Detailed fiches

### Family A — Kubernetes (existing) {#family-a}

#### 1024 — `use-of-probes` <a id="1024"></a>

* **Résumé factuel** : tout container long-running doit déclarer `livenessProbe` et `readinessProbe`.
* **État de l'art** : Sans probe de readiness, un Service expose un Pod qui n'est pas prêt → request-time errors + retry storm = surcharge. Sans liveness, un process bloqué consomme CPU/RAM sans servir — c'est le mode de défaillance documenté dans `[CNCF-Sustain-2023]` §4.2 comme "ghost workloads", responsables de 10–30 % de gaspillage observé en clusters production.
* **Mesure** : `[Datadog-Container-2024]` mentionne ~7 % des restarts comme dus à des liveness mal calibrées (côté faux positifs). Le gain net est donc une fois la règle adoptée avec un endpoint léger (`/healthz` < 5 ms).
* **Limites** : un mauvais endpoint de probe peut elle-même générer du CPU (anti-pattern : probe qui ouvre une connexion DB). À mesurer.
* **Sources** : `[K8s-Pod-Sec]`, `[CNCF-Sustain-2023]`, `[GSF-Patterns]` (pattern « kubernetes-health-probes »).

---

### Family B — Docker / OCI {#family-b}

#### 1025 — `multistage` <a id="1025"></a>

* **Résumé factuel** : un Dockerfile doit utiliser plusieurs `FROM` pour scinder build et runtime.
* **État de l'art** : Le multi-stage build (`[Docker-Multistage]`) éclate compilation et exécution → l'image finale ne contient que les artefacts et leurs runtimes. `[Chainguard-Wolfi]` chiffre la réduction sur des stacks Java/Node/Go à **−50 à −90 %** de taille compressée.
* **Mécanisme** : moins de couches OCI à transférer/déchiffrer → moins d'I/O réseau et CPU sur chaque pull (`[Liu-2021]`, axe 3).
* **Mesure** : OpenJDK-17-temurin (full JDK + Maven) ≈ 600 MB ; même app sur `eclipse-temurin:17-jre-jammy` après multi-stage ≈ 220 MB ; sur distroless `java17` ≈ 120 MB.
* **Limites** : les images mono-stage volontaires (build-only) sont exclues par feasibility ⚠️.
* **Sources** : `[Docker-Multistage]`, `[Chainguard-Wolfi]`, `[CNCF-Sustain-2023]`.

