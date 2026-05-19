_(Proposed rule for creedengo-infra — GCI1038.)_

### **Rule title**

One main process per container

### **Rule key**

GCI1038

### **Language and platform**

Docker / OCI

### **Rule description**

Containers are designed to host a single foreground process. Packing multiple long-running daemons via `supervisord`, `runit`, custom `start.sh` loops or trailing `&` in `CMD`/`ENTRYPOINT` confuses the kubelet (no proper liveness signal), prevents accurate HPA scaling per workload, defeats horizontal autoscaling, and wastes RAM/CPU on the supervisor itself.

_Noncompliant Code Example_

```Dockerfile
FROM debian:12-slim
RUN apt-get update && apt-get install -y supervisor nginx php-fpm \
 && rm -rf /var/lib/apt/lists/*
COPY supervisord.conf /etc/supervisor/conf.d/app.conf
CMD ["/usr/bin/supervisord", "-c", "/etc/supervisor/conf.d/app.conf"]
```

_Compliant Solution_

```Dockerfile
# Two separate Dockerfiles → two separate Deployments, each scaled independently
# Dockerfile.web
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/nginx.conf
CMD ["nginx", "-g", "daemon off;"]

# Dockerfile.php
FROM php:8.3-fpm-alpine
CMD ["php-fpm", "-F"]
```

### **Rule short description**

`CMD`/`ENTRYPOINT` must launch one foreground process; do not embed `supervisord`, `runit`, or background `&` in production images.

### **Rule justification**

**Why it matters**:
- HPA scales the **whole** pod — bundling 2 processes forces over-provisioning the silent one.
- Liveness/readiness can only signal one process; the other can be dead while the pod is `Ready`.

**Eco-design rationale** (axes 3·4·5·6):
- **Runtime energy**: independent scaling sizes each workload to its real demand → typically -20 % to -50 % aggregate CPU usage.
- **Memory efficiency**: scheduler bin-packs single-process containers more tightly; supervisors themselves cost 20–80 MB RAM each.
- **Scale-to-zero**: KEDA / Knative can scale individual workloads to zero — impossible with bundled processes.
- **Provisioning efficiency**: rolling updates roll one workload at a time, avoiding correlated downtime.

Sources:
- https://docs.docker.com/develop/develop-images/guidelines/#run-only-one-process-per-container
- https://kubernetes.io/docs/concepts/workloads/pods/
- https://keda.sh

Measurement:

| Indicator | Method | Order of magnitude |
| --- | --- | --- |
| Aggregate CPU | `kubectl top` before/after split | -20 % to -50 % |
| Idle RAM | supervisor process RSS | -20 to -80 MB per pod |

### **Severity / Remediation Cost**

Severity: **Info** — architectural recommendation; impact is significant but requires refactor.

Remediation cost: **Hard** — split into multiple images/Deployments, redesign networking.

### **Implementation principle**

- Target tech: Docker / OCI Dockerfile.
- Detection mechanism: sonar-iac Docker visitor (heuristic).
- Algorithm:

  ```
  cmd = lastStage.cmd() or lastStage.entrypoint()
  if cmd.text() matches /supervisord|runit|s6-svscan|honcho|foreman/:
      reportIssue(cmd, "Avoid process supervisors; one process per container")
  if cmd.text() matches /&\s*$|;\s*&/:
      reportIssue(cmd, "CMD/ENTRYPOINT should run a single foreground process")
  ```

- Known false positives: legitimate sidecar patterns (logrotate + app) — opt-out via comment.
- Feasibility verdict: ⚠️ heuristic (precision ~75 %).
- Alternative tooling: kube-linter `dangling-service`; manual review on architecture board.
