# Single region, no serverless-candidate resource: rule must stay silent.
provider "aws" {
  region = "eu-west-3"
}

resource "aws_lambda_function" "ingest" {
  function_name = "ingest"
  runtime       = "java21"
  handler       = "Handler::handle"
  role          = "arn:aws:iam::123456789012:role/lambda"
  filename      = "ingest.zip"
}

