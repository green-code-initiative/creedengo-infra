# Small / right-sized instances — no issue
resource "aws_instance" "small" {
  instance_type = "t4g.small"
}

# Oversized but justified by a sizing-rationale tag
resource "aws_instance" "big_justified" {
  instance_type = "m6i.2xlarge"
  tags = {
    sizing-rationale = "Compute Optimizer report 2026-04, ADR-12"
    environment      = "production"
  }
}

# Oversized but coupled with lifecycle.ignore_changes (recommender-managed)
resource "aws_instance" "big_managed" {
  instance_type = "m6i.4xlarge"
  lifecycle {
    ignore_changes = [instance_type]
  }
}

# Unknown size string — not flagged
resource "aws_instance" "interp" {
  instance_type = var.size
}

