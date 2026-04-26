#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SERVICE_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"

echo "==> Building auth-service JAR (skipping tests)..."
cd "$SERVICE_DIR"
./mvnw package -DskipTests -B -q

echo "==> Building Docker image: auth-service:dev..."
docker build -t auth-service:dev "$SERVICE_DIR"

echo "==> Applying Kubernetes manifests to dev namespace..."
kubectl apply -f "$SCRIPT_DIR/namespace.yaml"
kubectl apply -f "$SCRIPT_DIR/postgres.yaml"

echo "==> Waiting for PostgreSQL to be ready..."
kubectl rollout status deployment/postgres -n dev --timeout=90s

kubectl apply -f "$SCRIPT_DIR/auth-service.yaml"

echo "==> Waiting for auth-service to be ready..."
kubectl rollout status deployment/auth-service -n dev --timeout=120s

NODE_PORT=$(kubectl get svc auth-service -n dev -o jsonpath='{.spec.ports[0].nodePort}')
echo ""
echo "==> auth-service deployed to dev namespace"
echo "    Access at: http://localhost:${NODE_PORT}/api/v1/auth/register"
echo ""
echo "    Useful commands:"
echo "      kubectl logs -f deployment/auth-service -n dev"
echo "      kubectl get pods -n dev"
