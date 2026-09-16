variable "environment" {
  description = "Deployment environment represented by a Kubernetes namespace."
  type        = string

  validation {
    condition     = contains(["dev", "staging", "prod"], var.environment)
    error_message = "environment must be dev, staging, or prod."
  }
}

variable "kubeconfig" {
  description = "Path to the kubeconfig used by the local cluster."
  type        = string
  default     = "~/.kube/config"
}

variable "chart_path" {
  description = "Absolute path to the invoice Helm chart."
  type        = string
  default     = "../../../deploy/helm/invoice-api"
}

variable "image_repository" {
  type    = string
  default = "invoice-demo"
}

variable "image_tag" {
  type    = string
  default = "dev"
}

variable "image_pull_secret" {
  type    = string
  default = ""
}

variable "admin_password" {
  type      = string
  sensitive = true
  default   = "admin"
}

variable "jwt_secret" {
  type      = string
  sensitive = true
  default   = "local-invoice-demo-secret-32-bytes-minimum!!"
}
