terraform {
  required_version = ">= 1.5.0"

  required_providers {
    helm = {
      source  = "hashicorp/helm"
      version = "~> 2.17"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.35"
    }
  }
}

provider "kubernetes" {
  config_path = var.kubeconfig
}

provider "helm" {
  kubernetes {
    config_path = var.kubeconfig
  }
}

resource "kubernetes_namespace_v1" "environment" {
  metadata {
    name = "invoice-${var.environment}"
    labels = {
      "app.kubernetes.io/part-of" = "invoice-demo"
      environment                 = var.environment
    }
  }
}

resource "helm_release" "invoice_api" {
  name             = "invoice"
  namespace        = kubernetes_namespace_v1.environment.metadata[0].name
  create_namespace = false
  chart            = var.chart_path
  wait             = true
  timeout          = 600

  values = [
    file("${var.chart_path}/values.yaml"),
    file("${var.chart_path}/values-${var.environment}.yaml")
  ]

  set {
    name  = "api.image.repository"
    value = var.image_repository
  }

  set {
    name  = "api.image.tag"
    value = var.image_tag
  }

  dynamic "set" {
    for_each = var.image_pull_secret == "" ? [] : [var.image_pull_secret]
    content {
      name  = "imagePullSecrets[0].name"
      value = set.value
    }
  }

  set {
    name  = "app.adminPassword"
    value = var.admin_password
  }

  set_sensitive {
    name  = "app.jwtSecret"
    value = var.jwt_secret
  }
}
