# GCI1066 — `tfmultiregionandserverlesshint`

Two-axis Terraform check shipped as a single rule with **contextualised
messages**: it warns when a configuration deploys the same workload across
multiple regions of one cloud provider, and it hints to use a serverless
equivalent when a long-running compute resource is provisioned.

## Context

* **Eco-design axes** (see `doc/RULES-JUSTIFICATIONS.TEMPLATE.md` §0.2): #5
  *provisioning efficiency*, #6 *scale-to-zero / scale-to-fit*.
* **Severity**: `INFO` (advisory — the rule asks for confirmation, it does
  not block).
* **Type**: `CODE_SMELL`.

## Rule

The check raises one issue per detected pattern, with a message that names
the offending resource so a reviewer immediately knows what to act on.

### Axis A — Multi-region

Triggered when any of the following holds:

1. ≥ 2 `provider "<cloud>"` blocks with the same provider name and
   distinct `region` / `location` values.
2. A `resource` body containing `for_each = var.regions` (or
   `count = length(var.regions)`) where `each.value` / `count.index` flows
   into the `region` / `provider` attribute.
3. Explicit cross-region replication blocks such as
   `replication_configuration` on `aws_s3_bucket`,
   `aws_dynamodb_global_table`, `geo_redundant_*` on
   `azurerm_storage_account`, etc.

Silencer comment (on the line above the block): `# eco-design:multiregion=<reason>`.

### Axis B — Serverless hint

Triggered when a `resource` of one of the following types is declared:

| Provider | Detected resource                                      | Suggested serverless alternative              |
|----------|--------------------------------------------------------|------------------------------------------------|
| AWS      | `aws_instance`                                         | AWS Lambda / Fargate                          |
| AWS      | `aws_autoscaling_group` (`min_size > 0`)               | AWS Fargate / App Runner                      |
| AWS      | `aws_db_instance`                                      | Aurora Serverless v2                          |
| AWS      | `aws_elasticache_cluster`                              | ElastiCache Serverless                        |
| AWS      | `aws_ecs_service` (`launch_type = "EC2"`)              | `launch_type = "FARGATE"`                     |
| Azure    | `azurerm_virtual_machine`, `azurerm_linux_virtual_machine`, `azurerm_windows_virtual_machine` | Azure Functions / Container Apps |
| Azure    | `azurerm_mssql_database` (provisioned)                 | `serverless` SKU                              |
| GCP      | `google_compute_instance`                              | Cloud Run / Cloud Functions                   |
| GCP      | `google_sql_database_instance`                         | Cloud SQL serverless / Spanner Free           |
| GCP      | `google_container_node_pool` (`min_node_count > 0`)    | GKE Autopilot                                 |

Silencer comment: `# eco-design:serverless=<reason>`.

> Note — the user prompt mentioned "EC3"; the AWS service is **EC2**
> (`aws_instance`). The table above is data-driven (a `Map<String,String>`),
> so future families can be appended without changing the visitor logic.

## Non-compliant example

```hcl
# Reflex multi-region — no documented RPO/RTO.
provider "aws"               { region = "eu-west-3" }
provider "aws" { alias = "dr" region = "us-east-1" }

# Always-on VM where a Lambda would fit a bursty 5-min/hour load.
resource "aws_instance" "cron_runner" {
  instance_type = "t3.medium"
}
```

## Compliant example

```hcl
# eco-design:multiregion=GDPR data sovereignty (FR + DE)
provider "aws"               { region = "eu-west-3" }
provider "aws" { alias = "de" region = "eu-central-1" }

# eco-design:serverless=cold-start unacceptable for trading engine
resource "aws_instance" "trading_engine" {
  instance_type = "c7g.large"
}
```

## Sources

* Patterson, D. et al. — *The Carbon Footprint of ML Training…*, IEEE
  Computer, 2022. ★★★
* Masanet, E. et al. — *Recalibrating global data center energy-use
  estimates*, **Science** 367 (6481), 2020. ★★★
* Jonas, E. et al. — *A Berkeley View on Serverless Computing*, UC Berkeley
  EECS TR 2019-3 (arXiv:1902.03383). ★★★
* Castro, P. et al. — *The Rise of Serverless Computing*, **CACM** 62 (12),
  2019. ★★★
* Wang, L. et al. — *Peeking Behind the Curtains of Serverless Platforms*,
  USENIX ATC 2018. ★★★
* GSF — *Software Carbon Intensity (SCI) Specification v1.0* /
  ISO/IEC 21031:2024. ★★
* CNCF TAG-Environmental Sustainability — *Cloud Native Sustainability
  whitepaper*, 2023. ★★
* CNCF Serverless WG — *CNCF Serverless Whitepaper v1.0*. ★★
* AWS Well-Architected — *Sustainability Pillar* (SUS04-BP02, SUS05-BP01,
  SUS05-BP02), 2024. ★★
* Microsoft Cloud Adoption Framework — *Sustainability*, 2024. ★★
* Google Cloud Architecture Center — *Choosing a compute option*, 2024. ★
* Electricity Maps — annual grid-mix dataset. ★
* Google Cloud — *Carbon Footprint per region* dashboard. ★

