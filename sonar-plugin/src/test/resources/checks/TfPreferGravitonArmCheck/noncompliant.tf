resource "aws_instance" "web" {
  instance_type = "m6i.large"
  ami           = "ami-x86"
}

resource "aws_launch_template" "lt" {
  instance_type = "c5.xlarge"
}

resource "aws_db_instance" "rds" {
  instance_type = "r6i.large"
}

