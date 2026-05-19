_(Proposed rule for creedengo-infra — 1053.)_

### **Rule title**

Set storage lifecycle rules (tier transitions, expirations)

### **Rule key**

1053

### **Language and platform**

Terraform (AWS S3, GCS, Azure Blob)

### **Rule description**

Without lifecycle rules, object stores accumulate years of log files, build artefacts, backups and snapshots on the hot tier. Lifecycle rules move infrequently-accessed objects to `IA`, `Glacier`, `Coldline`, `Archive` tiers (5–10× less energy per stored byte) and eventually delete obsolete objects.

_Noncompliant Code Example_

```hcl
resource "aws_s3_bucket" "logs" {
  bucket = "myorg-app-logs"
}
# No aws_s3_bucket_lifecycle_configuration defined.
```

_Compliant Solution_

```hcl
resource "aws_s3_bucket" "logs" {
  bucket = "myorg-app-logs"
}
resource "aws_s3_bucket_lifecycle_configuration" "logs" {
  bucket = aws_s3_bucket.logs.id
  rule {
    id     = "transition-and-expire"
    status = "Enabled"
    transition  { days = 30,  storage_class = "STANDARD_IA" }
    transition  { days = 180, storage_class = "GLACIER" }
    expiration  { days = 730 }
  }
}
```

### **Rule short description**

Each `aws_s3_bucket` / `google_storage_bucket` / `azurerm_storage_container` must declare a matching lifecycle configuration / management policy.

### **Rule justification**

**Why it matters**:
- Hot storage (S3 Standard) costs ~$0.023/GB-month; Glacier Deep Archive: ~$0.00099/GB-month — and consumes proportionally less energy.
- Most logs / backups are never read after 30 days.

**Eco-design rationale** (axes 5·7):
- **Storage footprint**: archive tiers consume 5–10× less energy per stored byte than spinning hot storage.
- **Runtime energy**: less storage on hot tier → less replication, less indexing CPU.
- **Provisioning efficiency**: automatic deletion prevents unbounded growth.

Sources:
- https://aws.amazon.com/s3/storage-classes/
- https://cloud.google.com/storage/docs/lifecycle
- https://learn.microsoft.com/en-us/azure/storage/blobs/lifecycle-management-overview

Measurement:

| Indicator | Method | Order of magnitude |
| --- | --- | --- |
| Hot storage volume | provider metrics | -50 % to -95 % after 1 year |
| Storage cost | provider billing | -60 % to -90 % |

### **Severity / Remediation Cost**

Severity: **Minor**.

Remediation cost: **Easy** — declare lifecycle block.

### **Implementation principle**

- Target tech: Terraform HCL.
- Detection mechanism: sonar-iac Terraform visitor (cross-resource).
- Algorithm:

  ```
  for each aws_s3_bucket b in module:
      if no aws_s3_bucket_lifecycle_configuration references b.id: report(b, "Missing lifecycle configuration")
  # similar for google_storage_bucket + lifecycle_rule and azurerm_storage_management_policy
  ```

- Known false positives: short-lived buckets for ephemeral data — opt-out via tag.
- Feasibility verdict: ✅
- Alternative tooling: Checkov `CKV_AWS_300`; AWS Config rule.
