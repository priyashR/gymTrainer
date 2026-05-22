#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SERVICE_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"

echo "==> Building workout-session-service JAR (skipping tests)..."
cd "$SERVICE_DIR"
./mvnw package -DskipTests -B -q

echo "==> Building Docker image: workout-session-service:dev..."
docker build -t workout-session-service:dev "$SERVICE_DIR"

echo "==> Applying Kubernetes manifests to dev namespace..."
kubectl apply -f "$SCRIPT_DIR/namespace.yaml"
kubectl apply -f "$SCRIPT_DIR/rabbitmq.yaml"

echo "==> Waiting for RabbitMQ to be ready..."
kubectl rollout status deployment/rabbitmq -n dev --timeout=90s

kubectl apply -f "$SCRIPT_DIR/workout-session-service.yaml"

echo "==> Waiting for workout-session-service to be ready..."
kubectl rollout status deployment/workout-session-service -n dev --timeout=120s

NODE_PORT=$(kubectl get svc workout-session-service -n dev -o jsonpath='{.spec.ports[0].nodePort}')
echo ""
echo "==> workout-session-service deployed to dev namespace"
echo "    Access at: http://localhost:${NODE_PORT}/actuator/health"
echo ""
echo "    Useful commands:"
echo "      kubectl logs -f deployment/workout-session-service -n dev"
echo "      kubectl get pods -n dev"
