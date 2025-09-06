#!/bin/bash

# Docker build script with optimizations
set -e

echo "Building lite-gateway-sample Docker image..."

# Build with BuildKit for better performance
export DOCKER_BUILDKIT=1

# Build the image
docker build \
  --tag lite-gateway-sample:latest \
  --tag lite-gateway-sample:0.0.1-SNAPSHOT \
  --build-arg BUILDKIT_INLINE_CACHE=1 \
  --progress=plain \
  .

echo "Build completed successfully!"
echo "Image: lite-gateway-sample:latest"

# Optional: Show image size
echo "Image size:"
docker images lite-gateway-sample:latest --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}"

# Optional: Run the container for testing
read -p "Do you want to run the container now? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "Starting container..."
    docker run --rm -p 8080:8080 --name lite-gateway-sample-test lite-gateway-sample:latest
fi