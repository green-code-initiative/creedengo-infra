resource "aws_instance" "web" {
  instance_type = "m6i.2xlarge"
  ami           = "ami-x86"
}

resource "aws_db_instance" "rds" {
  instance_class = "db.r6i.4xlarge"
}

resource "google_compute_instance" "gcp_big" {
  machine_type = "n2-standard-16"
}

resource "azurerm_linux_virtual_machine" "az_big" {
  vm_size = "Standard_D16s_v3"
}

