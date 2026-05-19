_(Proposed rule for creedengo-infra — GCI1057.)_

### **Rule title**

CloudFormation: prefer Graviton & low-carbon region defaults

### **Rule key**

GCI1057

### **Language and platform**

AWS CloudFormation / SAM / CDK-synthesized templates

### **Rule description**

CloudFormation (and CDK synthesis output) mirrors the same anti-patterns as Terraform: `InstanceType: m6i.large` defaults, `eu-west-2` chosen "because we already had a VPC there" etc. This rule is the CloudFormation counterpart of `GCI1050` (Graviton) and `GCI1054` (low-carbon region), enforced on `AWS::EC2::*`, `AWS::AutoScaling::*`, `AWS::RDS::*`, `AWS::ECS::*` resources.

_Noncompliant Code Example_

```yaml
Resources:
  WebInstance:
    Type: AWS::EC2::Instance
    Properties:
      InstanceType: m6i.large
      ImageId: ami-x86_64
```

_Compliant Solution_

```yaml
Resources:
  WebInstance:
    Type: AWS::EC2::Instance
    Properties:
      InstanceType: m7g.large
      ImageId: !FindInMap [ Amis, !Ref "AWS::Region", arm64 ]
```

### **Rule short description**

CloudFormation compute/DB/ECS resources should default to Graviton (`m7g`, `c7g`, `r7g`, `db.t4g`, `db.r6g`, `db.m6g`) when supported.

### **Rule justification**

**Why it matters**: same as `GCI1050`.

**Eco-design rationale** (axes 3·5):
- **Runtime energy**: -20 % to -40 % at equivalent throughput (Graviton).
- **Provisioning efficiency**: same hourly cost or cheaper.

Sources:
- https://aws.amazon.com/ec2/graviton/
- https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/
- https://aws.amazon.com/architecture/well-architected/sustainability/

Measurement:

| Indicator | Method | Order of magnitude |
| --- | --- | --- |
| CPU·s per request | benchmark | -20 % to -40 % |

### **Severity / Remediation Cost**

Severity: **Minor**.

Remediation cost: **Medium** — multi-arch container builds, AMI mapping.

### **Implementation principle**

- Target tech: CloudFormation YAML / JSON.
- Detection mechanism: sonar-iac CloudFormation visitor.
- Algorithm:

  ```
  for each AWS::EC2::Instance / AWS::AutoScaling::LaunchConfiguration / LaunchTemplate / AWS::RDS::DBInstance r:
      if r.Properties.InstanceType matches X86_FAMILIES and an ARM equivalent exists:
          report(r, "Prefer Graviton equivalent")
  ```

- Known false positives: x86-only workloads — opt-out via tag.
- Feasibility verdict: ✅
- Alternative tooling: cfn-lint custom rule; AWS Compute Optimizer.
