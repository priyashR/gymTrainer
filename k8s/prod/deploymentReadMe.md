# Production Deployment Guide

Deploy the HybridStrength platform to a single-node k3s cluster.

---

## Prerequisites

- A running k3s cluster (single-node)
- `kubectl` configured to point at the k3s cluster
- Docker images pushed to Docker Hub (`priyash/gym-app:<service>-latest`)
- The k3s node must be able to pull from Docker Hub

### Verify cluster access

```bash
export KUBECONFIG=/path/to/your/k3s-kubeconfig
kubectl get nodes
```

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│  k3s cluster — prod namespace                               │
│                                                             │
│  ┌─────────────┐   ┌────────────────────────┐              │
│  │  PostgreSQL  │   │       RabbitMQ         │              │
│  │  :5432       │   │  :5672 (AMQP)         │              │
│  │  (ClusterIP) │   │  :15672 (Management)  │              │
│  └──────┬───────┘   │  (ClusterIP)          │              │
│         │           └───────────┬────────────┘              │
│         │                       │                           │
│  ┌──────┴───────────────────────┴──────────────────┐        │
│  │              Application Services               │        │
│  │  auth-service:8081 (ClusterIP)                  │        │
│  │  workout-creator-service:8082 (ClusterIP)       │        │
│  │  workout-session-service:8083 (ClusterIP)       │        │
│  └──────────────────────┬──────────────────────────┘        │
│                         │                                   │
│  ┌──────────────────────┴──────────────────────────┐        │
│  │  workout-coach-ui:80 (NodePort 30080)           │        │
│  │  nginx reverse-proxies /api/* to backend        │        │
│  └─────────────────────────────────────────────────┘        │
└─────────────────────────────────────────────────────────────┘

External access: http://<SERVER-IP>:30080
```

- Only the UI is exposed externally (NodePort 30080)
- All backend services are ClusterIP — accessible only within the cluster
- The UI's nginx.conf proxies API requests to backend services by their k8s DNS names

---

## Configuration

### Secrets — update before deploying

The manifests use placeholder passwords (`changeme-use-strong-password`). Update these in:

| File | Keys to update |
|------|---------------|
| `postgres.yaml` | `POSTGRES_PASSWORD` |
| `rabbitmq.yaml` | `RABBITMQ_DEFAULT_PASS` |
| `auth-service.yaml` | `DATABASE_PASSWORD` |
| `workout-creator-service.yaml` | `DB_PASSWORD` |
| `workout-session-service.yaml` | `DATABASE_PASSWORD`, `RABBITMQ_PASSWORD` |

**Important:** The DB password in service secrets must match `POSTGRES_PASSWORD` in `postgres.yaml`. The RabbitMQ password in `workout-session-service.yaml` must match `RABBITMQ_DEFAULT_PASS` in `rabbitmq.yaml`.

### JWT Keys

The public key is embedded in the workout-creator-service and workout-session-service secrets. The private key lives in the auth-service's `application-prod.yml` (baked into the image). If you rotate keys, update all three.

---

## Deployment

### Option A: One-shot deploy script

```bash
./k8s/prod/deploy.sh
```

This applies all manifests in order and waits for each component to become ready.

### Option B: Manual step-by-step

```bash
# 1. Create namespace
kubectl apply -f k8s/prod/namespace.yaml

# 2. Deploy infrastructure
kubectl apply -f k8s/prod/postgres.yaml
kubectl rollout status deployment/postgres -n prod --timeout=120s

kubectl apply -f k8s/prod/rabbitmq.yaml
kubectl rollout status deployment/rabbitmq -n prod --timeout=180s

# 3. Deploy application services
kubectl apply -f k8s/prod/auth-service.yaml
kubectl apply -f k8s/prod/workout-creator-service.yaml
kubectl apply -f k8s/prod/workout-session-service.yaml

# 4. Deploy UI
kubectl apply -f k8s/prod/workout-coach-ui.yaml

# 5. Wait for everything
kubectl rollout status deployment/auth-service -n prod --timeout=180s
kubectl rollout status deployment/workout-creator-service -n prod --timeout=180s
kubectl rollout status deployment/workout-session-service -n prod --timeout=180s
kubectl rollout status deployment/workout-coach-ui -n prod --timeout=60s
```

---

## Verification

```bash
# Check all pods are running
kubectl get pods -n prod

# Check services
kubectl get svc -n prod

# Test the UI
curl http://<SERVER-IP>:30080

# Test auth-service health (from within the cluster)
kubectl exec -it deployment/workout-coach-ui -n prod -- wget -qO- http://auth-service:8081/actuator/health
```

---

## Updating Services

When new images are pushed to Docker Hub (via CI on merge to main):

```bash
# Restart a single service to pull the latest image
kubectl rollout restart deployment/auth-service -n prod
kubectl rollout restart deployment/workout-creator-service -n prod
kubectl rollout restart deployment/workout-session-service -n prod
kubectl rollout restart deployment/workout-coach-ui -n prod
```

Or restart all at once:

```bash
kubectl rollout restart deployment -n prod
```

---

## Troubleshooting

### Pod not starting

```bash
kubectl describe pod <pod-name> -n prod
kubectl logs <pod-name> -n prod
```

### Service not connecting to Postgres

- Verify postgres pod is running: `kubectl get pods -n prod -l app=postgres`
- Check credentials match between postgres secret and service secrets
- Check Flyway migration logs: `kubectl logs deployment/auth-service -n prod | grep -i flyway`

### Service not connecting to RabbitMQ

- Verify rabbitmq pod is running: `kubectl get pods -n prod -l app=rabbitmq`
- Check credentials match between rabbitmq secret and session-service secret
- RabbitMQ management UI (if needed): `kubectl port-forward svc/rabbitmq 15672:15672 -n prod`

### UI loads but API calls fail

- Check backend services are healthy: `kubectl get pods -n prod`
- Verify service DNS names resolve (nginx proxies to `auth-service:8081`, `workout-creator-service:8082`, `workout-session-service:8083`)
- Check nginx logs: `kubectl logs deployment/workout-coach-ui -n prod`

---

## Resource Summary

| Component | CPU Request | Memory Request | CPU Limit | Memory Limit |
|-----------|-------------|----------------|-----------|--------------|
| PostgreSQL | 200m | 256Mi | 1000m | 512Mi |
| RabbitMQ | 200m | 256Mi | 500m | 512Mi |
| auth-service | 200m | 256Mi | 1000m | 512Mi |
| workout-creator-service | 200m | 256Mi | 1000m | 512Mi |
| workout-session-service | 200m | 256Mi | 1000m | 512Mi |
| workout-coach-ui | 50m | 64Mi | 200m | 128Mi |
| **Total** | **1050m** | **1344Mi** | **4700m** | **2688Mi** |

Ensure your k3s node has at least 4 CPU cores and 4GB RAM available for workloads.

---

## Teardown

```bash
kubectl delete namespace prod
```

This removes all resources (deployments, services, secrets, configmaps, PVCs) in one command. **This is destructive and will delete all data in the PVCs.**
