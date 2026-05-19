provider "aws" {
  region = "eu-north-1"
}

provider "google" {
  region = "europe-north1"
}

provider "azurerm" {
  location = "swedencentral"
  features {}
}

