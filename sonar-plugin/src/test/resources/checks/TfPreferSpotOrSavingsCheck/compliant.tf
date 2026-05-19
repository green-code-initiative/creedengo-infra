# AWS ASG with mixed_instances_policy (spot capacity declared)
resource "aws_autoscaling_group" "mixed" {
  min_size         = 1
  max_size         = 10
  mixed_instances_policy {
    instances_distribution {
      on_demand_base_capacity                  = 1
      on_demand_percentage_above_base_capacity = 20
      spot_allocation_strategy                 = "price-capacity-optimized"
    }
  }
}

# Azure VMSS with Spot priority
resource "azurerm_linux_virtual_machine_scale_set" "spot" {
  name     = "workers-vmss"
  sku      = "Standard_D2s_v3"
  priority = "Spot"
}

# GCP MIG with preemptible scheduling
resource "google_compute_instance_group_manager" "preempt" {
  name = "workers-mig"
  scheduling {
    provisioning_model = "SPOT"
  }
}

# Stateful workload — opt-out tag
resource "aws_autoscaling_group" "stateful_db_cluster" {
  min_size         = 3
  max_size         = 3
  desired_capacity = 3
  tags = {
    workload = "stateful"
  }
}

# Unrelated resource type — out of scope
resource "aws_s3_bucket" "logs" {
  bucket = "logs"
}

