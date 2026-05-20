resource "aws_db_instance" "staging" {
  identifier     = "staging-db"
  instance_class = "db.t3.large"
  engine         = "postgres"
  tags = {
    environment = "staging"
  }
}

resource "aws_instance" "dev_box" {
  instance_type = "t3.medium"
  tags = {
    environment = "dev"
  }
}

resource "azurerm_linux_virtual_machine" "qa" {
  name    = "qa-vm"
  vm_size = "Standard_D2_v3"
  tags = {
    env = "qa"
  }
}

