resource "aws_autoscaling_group" "workers" {
  min_size         = 1
  max_size         = 10
  desired_capacity = 2
  launch_template {
    id = "lt-xxx"
  }
}

resource "azurerm_linux_virtual_machine_scale_set" "vmss" {
  name = "workers-vmss"
  sku  = "Standard_D2s_v3"
}

resource "google_compute_instance_group_manager" "mig" {
  name = "workers-mig"
}

