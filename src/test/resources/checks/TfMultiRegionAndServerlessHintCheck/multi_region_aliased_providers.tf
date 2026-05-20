# Reflex multi-region: same provider name, two distinct regions.
provider "aws" {
  region = "eu-west-3"
}

provider "aws" {
  alias  = "dr"
  region = "us-east-1"
}

