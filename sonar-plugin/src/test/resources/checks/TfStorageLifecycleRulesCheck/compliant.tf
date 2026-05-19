resource "aws_s3_bucket" "logs" {
  bucket = "myorg-app-logs"
}

resource "aws_s3_bucket_lifecycle_configuration" "logs" {
  bucket = aws_s3_bucket.logs.id
  rule {
    id     = "expire-old"
    status = "Enabled"
    expiration {
      days = 730
    }
  }
}

resource "google_storage_bucket" "backups" {
  name     = "myorg-backups"
  location = "EU"
  lifecycle_rule {
    condition { age = 365 }
    action    { type = "Delete" }
  }
}

