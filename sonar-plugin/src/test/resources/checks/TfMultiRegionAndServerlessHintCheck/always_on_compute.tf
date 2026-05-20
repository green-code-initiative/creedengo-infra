# One always-on resource per cloud — each must trigger axis-B with a
# provider-specific suggestion.
resource "aws_instance" "cron_runner" {
  ami           = "ami-0123456789abcdef0"
  instance_type = "t3.medium"
}

resource "azurerm_linux_virtual_machine" "worker" {
  name                = "worker"
  resource_group_name = "rg"
  location            = "francecentral"
  size                = "Standard_B2s"
}

resource "google_compute_instance" "batch" {
  name         = "batch"
  machine_type = "e2-medium"
  zone         = "europe-west1-b"
}

