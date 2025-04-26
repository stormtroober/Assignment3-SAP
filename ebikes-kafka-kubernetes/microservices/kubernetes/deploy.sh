#!/bin/bash

set -e  # Exit on error

echo "Applying namespace..."
kubectl apply -f namespace.yml

echo "Applying Kubernetes manifests to 'ebikes' namespace..."
kubectl apply -f . --namespace=ebikes

echo "✅ Deployment complete."

