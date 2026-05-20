# Prod resource — no scheduling required
resource "aws_db_instance" "prod" {
  identifier     = "prod-db"
  instance_class = "db.r6g.large"
  tags = {
    environment = "production"
  }
}

# Untagged resource — out of scope (no environment hint)
resource "aws_instance" "anonymous" {
  instance_type = "t4g.micro"
}

# Non-prod resources alongside a scheduler — file-level exemption
resource "aws_db_instance" "staging" {
  identifier     = "staging-db"
  instance_class = "db.t3.large"
  tags = {
    environment = "staging"
  }
}

resource "aws_autoscaling_schedule" "stop_at_night" {
  scheduled_action_name = "stop-staging"
  min_size              = 0
  max_size              = 0
  desired_capacity      = 0
  recurrence            = "0 20 * * MON-FRI"
  autoscaling_group_name = "staging-asg"
}

# Non-prod but explicitly opted-out
resource "aws_instance" "batch_box" {
  instance_type = "t3.medium"
  tags = {
    environment = "dev"
    always_on   = "batch"
  }
}

