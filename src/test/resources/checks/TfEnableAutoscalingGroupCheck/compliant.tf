# Single instance (no count) — bastion / one-off pattern, out of scope
resource "aws_instance" "bastion" {
  instance_type = "t4g.nano"
  ami           = "ami-arm"
}

# count = 1 → not scaled out
resource "aws_instance" "single" {
  count         = 1
  instance_type = "t4g.micro"
}

# Already an autoscaling group → not in the BARE list, never flagged
resource "aws_autoscaling_group" "web" {
  min_size         = 2
  max_size         = 10
  desired_capacity = 3
}

# Managed instance group / VMSS equivalents
resource "azurerm_linux_virtual_machine_scale_set" "vmss" {
  sku       = "Standard_D2_v3"
  instances = 3
}


