resource "aws_instance" "workers" {
  count         = 4
  instance_type = "m7g.large"
  ami           = "ami-arm"
}

resource "google_compute_instance" "fleet" {
  for_each     = toset(["a", "b", "c"])
  machine_type = "n2-standard-2"
}

resource "azurerm_linux_virtual_machine" "scaled" {
  count   = var.replicas
  name    = "vm-${count.index}"
  vm_size = "Standard_D2_v3"
}

