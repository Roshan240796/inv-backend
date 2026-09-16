# Local Kubernetes Environments

This project includes a Helm chart and Terraform configuration for local Kubernetes. Each environment is isolated in its own namespace:

| Environment | Namespace | API NodePort |
| --- | --- | --- |
| dev | `invoice-dev` | `30080` |
| staging | `invoice-staging` | `30081` |
| prod | `invoice-prod` | `30082` |

## GitLab CI deployment

GitLab CI can deploy this cluster when the GitLab Runner runs inside the same WSL environment as Kubernetes. A shared GitLab runner cannot access a cluster bound only to WSL localhost.

Register a self-hosted runner with the `wsl-k8s` tag, using the Shell executor. The runner host needs `docker`, `kubectl`, `terraform`, and `helm` on its `PATH`. The runner user must also be able to access the cluster with the kubeconfig used by `kubectl`.

Add these masked and protected GitLab CI/CD variables:

- `KUBE_CONFIG_B64`: base64-encoded kubeconfig for the WSL cluster
- `APP_ADMIN_PASSWORD`: environment admin password
- `JWT_SECRET`: long random JWT signing secret

The pipeline in `.gitlab-ci.yml` validates Helm and Terraform, pushes the image to the GitLab Container Registry, and deploys dev automatically from the default branch. Staging is manual from the default branch, and production is manual from a Git tag.

Local Terraform commands use a state file in `infra/terraform/local/terraform.tfstate`. GitLab CI creates a temporary HTTP backend configuration and stores each environment remotely in GitLab. The CI job receives the backend configuration automatically through these variables:

```bash
export TF_HTTP_ADDRESS="$CI_API_V4_URL/projects/$CI_PROJECT_ID/terraform/state/local"
export TF_HTTP_USERNAME="$CI_REGISTRY_USER"
export TF_HTTP_PASSWORD="$CI_JOB_TOKEN"
export TF_HTTP_LOCK_ADDRESS="$TF_HTTP_ADDRESS/lock"
export TF_HTTP_UNLOCK_ADDRESS="$TF_HTTP_ADDRESS/lock"
export TF_HTTP_LOCK_METHOD=POST
export TF_HTTP_UNLOCK_METHOD=DELETE
```

The pipeline uses separate state names: `dev`, `staging`, and `prod`. For local testing, do not set `TF_HTTP_ADDRESS`; Terraform will use its local state file.

## Prerequisites

Install Docker, `kubectl`, Terraform, Helm, and either `kind` or Minikube.

## Create a cluster

With kind:

```bash
kind create cluster --name invoice-local
```

With Minikube:

```bash
minikube start
```

## Build and load the backend image

From the repository root:

```bash
docker build -t invoice-demo:dev .
kind load docker-image invoice-demo:dev --name invoice-local
```

For Minikube, build inside Minikube's Docker daemon instead:

```bash
eval "$(minikube docker-env)"
docker build -t invoice-demo:dev .
```

## Deploy an environment

Terraform uses the Helm chart and creates the environment namespace:

```bash
cd infra/terraform/local
terraform init
terraform apply -var-file=dev.tfvars
```

For local testing, use `terraform init`. In GitLab CI, the pipeline uses `terraform init -reconfigure` after creating the HTTP backend configuration.

Use `staging.tfvars` or `prod.tfvars` to deploy the other environments. Set real secrets on the command line or in an ignored `.tfvars` file for anything beyond local development:

```bash
terraform apply -var-file=prod.tfvars \
  -var='admin_password=replace-me' \
  -var='jwt_secret=replace-with-a-long-random-secret'
```

Check the deployment:

```bash
kubectl -n invoice-dev get pods,svc,pvc
kubectl -n invoice-dev rollout status deployment/invoice-invoice-api
```

For kind, use port forwarding from WSL:

```bash
kubectl -n invoice-dev port-forward service/invoice-invoice-api 8080:8080
```

The API is then available at `http://localhost:8080`. For Minikube, use:

```bash
minikube service invoice-invoice-api -n invoice-dev --url
```

For Android Emulator development, pass the resolved API URL to Flutter:

```bash
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080
```

With the kind port-forward, use `http://10.0.2.2:8080` in the Android Emulator. With Minikube, use the URL returned by `minikube service`; for a physical device, use an address reachable from the device instead of `10.0.2.2`.

## Remove an environment

```bash
terraform destroy -var-file=dev.tfvars
```

Destroying the release also removes its PostgreSQL persistent volume claim. This configuration is intended for local development, not production data retention.
