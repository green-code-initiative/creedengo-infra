resource "aws_s3_bucket" "logs" {
  bucket = "myorg-app-logs"
}

resource "google_storage_bucket" "backups" {
  name     = "myorg-backups"
  location = "EU"
}

