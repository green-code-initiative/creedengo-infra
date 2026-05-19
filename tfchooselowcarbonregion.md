_(Proposed rule for creedengo-infra — GCI1054.)_

### **Rule title**

Prefer low-carbon cloud regions

### **Rule key**

GCI1054

### **Language and platform**

Terraform (AWS, GCP, Azure)

### **Rule description**

Cloud regions vary enormously in carbon intensity of their electricity mix: **Sweden / Switzerland / Quebec** (≤ 50 gCO₂/kWh) vs **Singapore / India / South Africa** (500–800 gCO₂/kWh). For non-latency-critical workloads, choosing a low-carbon region is the highest-leverage eco-design lever in infra.

_Noncompliant Code Example_

```hcl
provider "aws"   { region = "ap-southeast-1" }   # Singapore ~ 400 gCO₂/kWh
provider "google"{ region = "asia-south1"   }    # Mumbai    ~ 700 gCO₂/kWh
```

_Compliant Solution_

```hcl
# eco-design: region chosen for low carbon intensity (Electricity Maps 2026-04)
provider "aws"   { region = "eu-north-1" }   # Stockholm ~  30 gCO₂/kWh
provider "google"{ region = "europe-north1"} # Hamina    ~  80 gCO₂/kWh
```

### **Rule short description**

Workloads not bound to a specific geography should declare their `region` / `location` based on grid carbon intensity. The rule flags providers configured in high-carbon regions without justification.

### **Rule justification**

**Why it matters**:
- AWS official region-CO₂ data and Electricity Maps daily averages show 10–25× spread.
- Moving a non-critical workload from `ap-southeast-1` to `eu-north-1` divides its **scope 2** emissions by ~10.

**Eco-design rationale** (axis 5 — and most fundamentally axis 3 carbon intensity):
- **Runtime energy** does not change, but the **carbon per kWh** does.
- Aligns with the Green Software Foundation SCI specification.

Sources:
- https://app.electricitymaps.com/
- https://sustainability.aboutamazon.com/products-services/aws-cloud
- https://cloud.google.com/sustainability/region-carbon
- https://greensoftware.foundation/articles/sci-specification

Measurement:

| Indicator | Method | Order of magnitude |
| --- | --- | --- |
| Carbon intensity | Electricity Maps API | -50 % to -95 % |

### **Severity / Remediation Cost**

Severity: **Info** (region choice has many other drivers).

Remediation cost: **Easy** — change the region string, but think about data sovereignty / latency.

### **Implementation principle**

- Target tech: Terraform HCL.
- Detection mechanism: sonar-iac Terraform visitor + static lookup table.
- Algorithm:

  ```
  CARBON_TABLE = embedded data file regions.json (avg gCO2/kWh, source: cloud provider + electricity maps)
  HIGH_CARBON_THRESHOLD = property("creedengo.infra.tf.region.thresholdGramsPerKwh", default=300)
  for each provider block (aws/google/azurerm) and resource override of region/location:
      region = parsedRegion
      if CARBON_TABLE[region] > HIGH_CARBON_THRESHOLD:
          if no `# eco-design:region` justification comment above: report(provider, "High-carbon region; document rationale or relocate")
  ```

- Known false positives: legitimate data-sovereignty constraints (GDPR, HIPAA jurisdictions) — silence via `# eco-design:region=<reason>`.
- Feasibility verdict: ✅
- Alternative tooling: Cloud Carbon Footprint, Climatiq.
