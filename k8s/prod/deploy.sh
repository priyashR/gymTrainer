#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "==> Creating prod namespace..."
kubectl apply -f "$SCRIPT_DIR/namespace.yaml"

echo "==> Deploying PostgreSQL..."
kubectl apply -f "$SCRIPT_DIR/postgres.yaml"
echo "    Waiting for PostgreSQL to be ready..."
kubectl rollout status deployment/postgres -n prod --timeout=120s

echo "==> Deploying RabbitMQ..."
kubectl apply -f "$SCRIPT_DIR/rabbitmq.yaml"
echo "    Waiting for RabbitMQ to be ready..."
kubectl rollout status deployment/rabbitmq -n prod --timeout=180s

echo "==> Deploying auth-service..."
kubectl apply -f "$SCRIPT_DIR/auth-service.yaml"

echo "==> Deploying workout-creator-service..."
kubectl apply -f "$SCRIPT_DIR/workout-creator-service.yaml"

echo "==> Deploying workout-session-service..."
kubectl apply -f "$SCRIPT_DIR/workout-session-service.yaml"

echo "==> Deploying workout-coach-ui..."
kubectl apply -f "$SCRIPT_DIR/workout-coach-ui.yaml"

echo ""
echo "==> Waiting for services to come up..."
kubectl rollout status deployment/auth-service -n prod --timeout=180s
kubectl rollout status deployment/workout-creator-service -n prod --timeout=180s
kubectl rollout status deployment/workout-session-service -n prod --timeout=180s
kubectl rollout status deployment/workout-coach-ui -n prod --timeout=60s

echo ""
echo "============================================"
echo "  Production deployment complete!"
echo "============================================"
echo ""
echo "  UI:  http://<SERVER-IP>:30080"
echo ""
echo "  Useful commands:"
echo "    kubectl get pods -n prod"
echo "    kubectl logs -f deployment/auth-service -n prod"
echo "    kubectl logs -f deployment/workout-creator-service -n prod"
echo "    kubectl logs -f deployment/workout-session-service -n prod"
echo ""
