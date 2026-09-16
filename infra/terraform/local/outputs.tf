output "namespace" {
  value = kubernetes_namespace_v1.environment.metadata[0].name
}

output "api_service" {
  value = "invoice-invoice-api"
}
