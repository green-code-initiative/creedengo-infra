#!/usr/bin/env bash
# eco-iac-scan.sh
# ------------------------------------------------------------------------
# Orchestrates external static + dynamic IaC analyzers that complement the
# creedengo-infra Sonar rules (GCI1020-GCI1099).
#
# Per-rule tooling matrix: see ../doc/RULES-EXTERNAL-TOOLING.md
#
# Usage:
#   ./tools/eco-iac-scan.sh [--root PATH] [--image NAME:TAG] [--skip STAGE...]
#                           [--report-dir DIR] [--fail-on SEV]
#
#   STAGE ∈ {docker,image,k8s,helm,terraform,cfn,ci,secrets}
#   SEV   ∈ {none,info,low,medium,high,critical}  (default: high)
#
# Environment:
#   ECO_IAC_DOCKERFILES   glob override, default: "Dockerfile*"
#   ECO_IAC_K8S_DIRS      space-separated dirs containing K8s YAML
#   ECO_IAC_HELM_DIRS     space-separated Helm chart dirs (containing Chart.yaml)
#   ECO_IAC_TF_DIRS       space-separated Terraform roots
#   ECO_IAC_CFN_GLOB      glob for CFN templates, default: "*.cfn.yml *.cfn.yaml"
#
# Exit code: non-zero if any enabled scanner reports an issue at or above
# --fail-on. All tool reports are written under $REPORT_DIR even on failure.
# ------------------------------------------------------------------------

set -Eeuo pipefail

ROOT="$(pwd)"
IMAGE=""
REPORT_DIR="target/eco-iac-reports"
FAIL_ON="high"
declare -a SKIP=()

usage() { sed -n '2,/^# ---/p' "$0" | sed 's/^# \{0,1\}//'; exit "${1:-0}"; }

while [[ $# -gt 0 ]]; do
  case "$1" in
    --root)        ROOT="$2"; shift 2 ;;
    --image)       IMAGE="$2"; shift 2 ;;
    --report-dir)  REPORT_DIR="$2"; shift 2 ;;
    --fail-on)     FAIL_ON="$2"; shift 2 ;;
    --skip)        SKIP+=("$2"); shift 2 ;;
    -h|--help)     usage 0 ;;
    *)             echo "unknown arg: $1" >&2; usage 1 ;;
  esac
done

cd "$ROOT"
mkdir -p "$REPORT_DIR"
GLOBAL_RC=0

# Colors when interactive
if [[ -t 1 ]]; then C_B='\033[1m'; C_G='\033[32m'; C_Y='\033[33m'; C_R='\033[31m'; C_0='\033[0m'
else C_B=''; C_G=''; C_Y=''; C_R=''; C_0=''
fi

log()  { printf "${C_B}[eco-iac]${C_0} %s\n" "$*"; }
ok()   { printf "${C_G}  ✓${C_0} %s\n" "$*"; }
warn() { printf "${C_Y}  ⚠${C_0} %s\n" "$*"; }
fail() { printf "${C_R}  ✗${C_0} %s\n" "$*"; GLOBAL_RC=1; }

have() { command -v "$1" >/dev/null 2>&1; }
skipped() { local s; for s in "${SKIP[@]:-}"; do [[ "$s" == "$1" ]] && return 0; done; return 1; }

# ------------------------------------------------------------------------
# Discovery
# ------------------------------------------------------------------------
DOCKERFILES=()
while IFS= read -r -d '' f; do DOCKERFILES+=("$f"); done < <(
  find . -type f \( -name 'Dockerfile' -o -name 'Dockerfile.*' -o -name '*.Dockerfile' \) \
       -not -path './**/target/*' -not -path './**/node_modules/*' -print0 2>/dev/null
)

K8S_FILES=()
HELM_DIRS=()
if [[ -n "${ECO_IAC_HELM_DIRS:-}" ]]; then
  read -r -a HELM_DIRS <<< "$ECO_IAC_HELM_DIRS"
else
  while IFS= read -r -d '' f; do HELM_DIRS+=("$(dirname "$f")"); done < <(
    find . -type f -name 'Chart.yaml' -not -path './**/target/*' -print0 2>/dev/null
  )
fi
while IFS= read -r -d '' f; do
  # Heuristic: K8s manifests contain `kind:` at root.
  if grep -qE '^kind:[[:space:]]+(Deployment|StatefulSet|DaemonSet|Pod|Job|CronJob|Service|Ingress|HorizontalPodAutoscaler|PodDisruptionBudget|ConfigMap|Secret|NetworkPolicy)' "$f" 2>/dev/null; then
    K8S_FILES+=("$f")
  fi
done < <(find . -type f \( -name '*.yml' -o -name '*.yaml' \) \
              -not -path '*/.github/*' -not -path '*/target/*' \
              -not -path '*/templates/*' -print0 2>/dev/null)

TF_DIRS=()
if [[ -n "${ECO_IAC_TF_DIRS:-}" ]]; then
  read -r -a TF_DIRS <<< "$ECO_IAC_TF_DIRS"
else
  while IFS= read -r -d '' f; do TF_DIRS+=("$(dirname "$f")"); done < <(
    find . -type f -name '*.tf' -not -path './**/.terraform/*' -print0 2>/dev/null
  )
  # uniq
  if [[ ${#TF_DIRS[@]} -gt 0 ]]; then
    IFS=$'\n' TF_DIRS=($(printf '%s\n' "${TF_DIRS[@]}" | sort -u)); unset IFS
  fi
fi

CFN_FILES=()
while IFS= read -r -d '' f; do
  if grep -qE '^(AWSTemplateFormatVersion|Transform:[[:space:]]*AWS::Serverless)' "$f" 2>/dev/null; then
    CFN_FILES+=("$f")
  fi
done < <(find . -type f \( -name '*.yml' -o -name '*.yaml' -o -name '*.json' \) \
              -not -path '*/target/*' -not -path '*/.github/*' -print0 2>/dev/null)

GHA_FILES=(); [[ -d .github/workflows ]] && \
  while IFS= read -r -d '' f; do GHA_FILES+=("$f"); done < <(
    find .github/workflows -type f \( -name '*.yml' -o -name '*.yaml' \) -print0
  )
GITLAB_CI=""; [[ -f .gitlab-ci.yml ]] && GITLAB_CI=".gitlab-ci.yml"

log "Discovery summary"
echo "  Dockerfiles : ${#DOCKERFILES[@]}"
echo "  K8s files   : ${#K8S_FILES[@]}"
echo "  Helm charts : ${#HELM_DIRS[@]}"
echo "  Terraform   : ${#TF_DIRS[@]}"
echo "  CFN files   : ${#CFN_FILES[@]}"
echo "  GH Actions  : ${#GHA_FILES[@]}"
echo "  GitLab CI   : ${GITLAB_CI:-none}"
echo "  Image       : ${IMAGE:-<none>}"

# ------------------------------------------------------------------------
# Stages
# ------------------------------------------------------------------------

stage_docker() {
  skipped docker && { warn "skip stage: docker"; return; }
  [[ ${#DOCKERFILES[@]} -eq 0 ]] && { warn "no Dockerfiles"; return; }
  log "Stage: docker  (GCI1025-GCI1039)"

  if have hadolint; then
    for df in "${DOCKERFILES[@]}"; do
      out="$REPORT_DIR/hadolint-$(echo "$df" | tr '/.' '__').json"
      if hadolint --format json "$df" > "$out" 2>/dev/null; then ok "hadolint $df"
      else fail "hadolint reported issues for $df ($out)"; fi
    done
  else warn "hadolint not installed"; fi

  if have trivy; then
    out="$REPORT_DIR/trivy-config-docker.json"
    trivy config --quiet --format json --output "$out" \
      --severity "${FAIL_ON^^},CRITICAL" \
      "$(dirname "${DOCKERFILES[0]}")" 2>/dev/null \
      && ok "trivy config (docker)" \
      || fail "trivy config (docker) reported issues ($out)"
  else warn "trivy not installed"; fi

  if have checkov; then
    out="$REPORT_DIR/checkov-docker.json"
    checkov -d . --framework dockerfile -o json --output-file-path "$REPORT_DIR/" --soft-fail >/dev/null 2>&1
    [[ -f "$REPORT_DIR/results_json.json" ]] && mv "$REPORT_DIR/results_json.json" "$out" && ok "checkov dockerfile"
  fi
}

stage_image() {
  skipped image && { warn "skip stage: image"; return; }
  [[ -z "$IMAGE" ]] && { warn "no --image provided, skipping dynamic image scan"; return; }
  log "Stage: image  (GCI1026/1028/1032/1035/1036)"

  if have trivy; then
    out="$REPORT_DIR/trivy-image.json"
    trivy image --quiet --format json --output "$out" \
      --severity "${FAIL_ON^^},CRITICAL" "$IMAGE" \
      && ok "trivy image $IMAGE" \
      || fail "trivy image reported issues ($out)"
  else warn "trivy not installed"; fi

  if have dive; then
    out="$REPORT_DIR/dive-$IMAGE.txt"
    CI=true dive "$IMAGE" --ci-config <(cat <<-'YAML'
		rules:
		  lowestEfficiency: 0.95
		  highestWastedBytes: 50MB
		  highestUserWastedPercent: 0.10
		YAML
		) > "$out" 2>&1 \
      && ok "dive efficiency OK ($out)" \
      || fail "dive efficiency below threshold ($out)"
  else warn "dive not installed"; fi
}

stage_k8s() {
  skipped k8s && { warn "skip stage: k8s"; return; }
  [[ ${#K8S_FILES[@]} -eq 0 ]] && { warn "no K8s manifests"; return; }
  log "Stage: k8s  (GCI1024/1040-1048)"

  if have kube-linter; then
    out="$REPORT_DIR/kube-linter.sarif"
    kube-linter lint --format sarif "${K8S_FILES[@]}" > "$out" 2>/dev/null \
      && ok "kube-linter" \
      || fail "kube-linter reported issues ($out)"
  else warn "kube-linter not installed"; fi

  if have kube-score; then
    out="$REPORT_DIR/kube-score.txt"
    kube-score score --output-format ci "${K8S_FILES[@]}" > "$out" 2>&1 \
      && ok "kube-score" \
      || fail "kube-score reported issues ($out)"
  else warn "kube-score not installed"; fi

  if have checkov; then
    checkov -d . --framework kubernetes -o json \
      --output-file-path "$REPORT_DIR/checkov-k8s/" --soft-fail >/dev/null 2>&1 \
      && ok "checkov kubernetes"
  fi
}

stage_helm() {
  skipped helm && { warn "skip stage: helm"; return; }
  [[ ${#HELM_DIRS[@]} -eq 0 ]] && { warn "no Helm charts"; return; }
  log "Stage: helm  (GCI1049 + render-then-lint for GCI1040-1048)"
  have helm || { warn "helm not installed"; return; }

  for chart in "${HELM_DIRS[@]}"; do
    rendered="$REPORT_DIR/helm-rendered-$(basename "$chart").yaml"
    if helm template eco "$chart" --include-crds > "$rendered" 2>/dev/null; then
      ok "helm template $chart"
      if have kube-linter; then
        kube-linter lint --format sarif "$rendered" \
          > "$REPORT_DIR/kube-linter-helm-$(basename "$chart").sarif" 2>/dev/null \
          || fail "kube-linter (helm) issues in $chart"
      fi
      if have kube-score; then
        kube-score score "$rendered" \
          > "$REPORT_DIR/kube-score-helm-$(basename "$chart").txt" 2>&1 \
          || fail "kube-score (helm) issues in $chart"
      fi
    else
      fail "helm template failed for $chart"
    fi
  done
}

stage_terraform() {
  skipped terraform && { warn "skip stage: terraform"; return; }
  [[ ${#TF_DIRS[@]} -eq 0 ]] && { warn "no Terraform"; return; }
  log "Stage: terraform  (GCI1050-1056)"

  for d in "${TF_DIRS[@]}"; do
    if have tflint; then
      (cd "$d" && tflint --format json) > "$REPORT_DIR/tflint-$(echo "$d" | tr '/.' '__').json" 2>/dev/null \
        && ok "tflint $d" \
        || fail "tflint reported issues in $d"
    fi
    if have tfsec; then
      tfsec --format json --out "$REPORT_DIR/tfsec-$(echo "$d" | tr '/.' '__').json" "$d" >/dev/null 2>&1 \
        && ok "tfsec $d" \
        || fail "tfsec reported issues in $d"
    fi
  done

  if have checkov; then
    checkov -d . --framework terraform -o json \
      --output-file-path "$REPORT_DIR/checkov-tf/" --soft-fail >/dev/null 2>&1 \
      && ok "checkov terraform"
  fi
}

stage_cfn() {
  skipped cfn && { warn "skip stage: cfn"; return; }
  [[ ${#CFN_FILES[@]} -eq 0 ]] && { warn "no CloudFormation"; return; }
  log "Stage: cfn  (GCI1057)"

  if have cfn-lint; then
    out="$REPORT_DIR/cfn-lint.json"
    cfn-lint -f json "${CFN_FILES[@]}" > "$out" 2>/dev/null \
      && ok "cfn-lint" \
      || fail "cfn-lint reported issues ($out)"
  else warn "cfn-lint not installed"; fi

  if have checkov; then
    checkov -d . --framework cloudformation -o json \
      --output-file-path "$REPORT_DIR/checkov-cfn/" --soft-fail >/dev/null 2>&1 \
      && ok "checkov cfn"
  fi
}

stage_ci() {
  skipped ci && { warn "skip stage: ci"; return; }
  log "Stage: ci  (GCI1058-1060, GCI1065)"

  if [[ ${#GHA_FILES[@]} -gt 0 ]]; then
    if have actionlint; then
      out="$REPORT_DIR/actionlint.txt"
      actionlint -format '{{json .}}' "${GHA_FILES[@]}" > "$out" 2>&1 \
        && ok "actionlint" \
        || fail "actionlint reported issues ($out)"
    else warn "actionlint not installed"; fi

    if have pinact; then
      out="$REPORT_DIR/pinact.txt"
      pinact run --check "${GHA_FILES[@]}" > "$out" 2>&1 \
        && ok "pinact (actions pinned to SHA)" \
        || fail "pinact: unpinned actions detected ($out)"
    fi
  fi

  if [[ -n "$GITLAB_CI" ]] && have glab; then
    out="$REPORT_DIR/glab-ci-lint.txt"
    glab ci lint --path "$GITLAB_CI" > "$out" 2>&1 \
      && ok "glab ci lint" \
      || fail "glab ci lint reported issues ($out)"
  fi
}

stage_secrets() {
  skipped secrets && { warn "skip stage: secrets"; return; }
  log "Stage: secrets  (GCI1061)"

  if have gitleaks; then
    out="$REPORT_DIR/gitleaks.json"
    gitleaks detect --no-banner --redact --report-format json --report-path "$out" \
      --exit-code 1 > /dev/null 2>&1 \
      && ok "gitleaks (no secrets)" \
      || fail "gitleaks detected secrets ($out)"
  elif have trufflehog; then
    trufflehog filesystem . --json > "$REPORT_DIR/trufflehog.json" 2>/dev/null \
      && ok "trufflehog (no secrets)" \
      || fail "trufflehog detected secrets"
  else
    warn "no secret scanner installed (gitleaks/trufflehog)"
  fi
}

# ------------------------------------------------------------------------
# Run pipeline
# ------------------------------------------------------------------------
stage_docker
stage_image
stage_k8s
stage_helm
stage_terraform
stage_cfn
stage_ci
stage_secrets

log "Reports written to: $REPORT_DIR"
if [[ $GLOBAL_RC -ne 0 ]]; then
  printf "${C_R}eco-iac-scan: issues detected (fail-on=%s)${C_0}\n" "$FAIL_ON"
else
  printf "${C_G}eco-iac-scan: clean${C_0}\n"
fi
exit "$GLOBAL_RC"

