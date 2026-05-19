# All-Graviton — no issue should be raised.
resource "aws_instance" "web" {
  instance_type = "m7g.large"
  ami           = "ami-arm64"
}

resource "aws_instance" "worker" {
  instance_type = "t4g.medium"
  ami           = "ami-arm64"
}

resource "aws_launch_template" "lt" {
  instance_type = "c7g.xlarge"
}

# Non-target resource — must not be flagged.
resource "aws_security_group" "sg" {
  name = "demo"
}

