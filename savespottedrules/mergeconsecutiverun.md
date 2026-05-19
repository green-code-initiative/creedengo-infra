_(Proposed rule for creedengo-infra — 1032.)_

### **Rule title**

Merge consecutive `RUN` instructions

### **Rule key**

1032

### **Language and platform**

Docker / OCI

### **Rule description**

Each `RUN` instruction produces a layer. Consecutive small `RUN` instructions multiply layer metadata, inflate image size and slow pulls. Merging related `RUN`s with `&&` and `\` continuations compresses layers and lets you delete temporary files within the same layer (e.g., `apt-get update && apt-get install … && rm -rf /var/lib/apt/lists/*`).

_Noncompliant Code Example_

```Dockerfile
FROM debian:12-slim
RUN apt-get update
RUN apt-get install -y curl
RUN apt-get install -y jq
RUN rm -rf /var/lib/apt/lists/*
```

_Compliant Solution_

```Dockerfile
FROM debian:12-slim
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl jq \
 && rm -rf /var/lib/apt/lists/*
```

### **Rule short description**

Group related shell commands inside a single `RUN`; delete artefacts in the same layer that created them.

### **Rule justification**

**Why it matters**:
- Each layer adds metadata overhead and forces an extra `tar` round-trip on every push/pull.
- Files deleted in a later `RUN` still occupy space in the earlier layer ("layer fossilisation"): the final image keeps them.

**Eco-design rationale** (axes 1·3·5·7):
- **Image size reduction**: merging often saves 50–300 MB by letting `rm -rf` happen in-layer.
- **Runtime energy**: fewer layers → faster decompression at container start; on a 50-pod scale-up event, this is measurable in seconds and Wh.
- **Provisioning efficiency**: shorter image pulls accelerate Karpenter/Cluster Autoscaler reactivity to load spikes.
- **Storage footprint**: registries store fewer (but larger) blobs, improving deduplication and reducing per-layer storage overhead.

Sources:
- https://docs.docker.com/develop/develop-images/guidelines/#minimize-the-number-of-layers
- https://docs.docker.com/build/cache/optimize/
- https://github.com/hadolint/hadolint/wiki/DL3059

Measurement:

| Indicator | Method | Order of magnitude |
| --- | --- | --- |
| Layer count | `docker history <image>` | -30 % to -70 % |
| Image size | `docker images` | -10 % to -40 % |

### **Severity / Remediation Cost**

Severity: **Minor** — cumulative impact across many images.

Remediation cost: **Medium** — refactor several `RUN`s into a single chained command, taking care of shell exit codes.

### **Implementation principle**

- Target tech: Docker / OCI Dockerfile.
- Detection mechanism: sonar-iac Docker visitor.
- Algorithm:

  ```
  for each stage in file.stages():
      for each window of 2+ consecutive RunInstruction in stage.instructions():
          if no other instruction between them:
              reportIssue(window[0], "Merge consecutive RUN instructions to reduce layers")
  ```

- Known false positives: dev images where layer separation is intentional for caching specific steps.
- Feasibility verdict: ✅
- Alternative tooling: Hadolint **DL3059**.
