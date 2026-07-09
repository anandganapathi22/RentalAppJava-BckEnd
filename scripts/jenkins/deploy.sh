#!/usr/bin/env bash
set -euo pipefail

environment="${1:-}"
artifact_pattern="${2:-}"

if [[ -z "$environment" || -z "$artifact_pattern" ]]; then
  echo "Usage: $0 <dev|stage|prod> <artifact-pattern>"
  exit 2
fi

case "$environment" in
  dev|stage|prod) ;;
  *)
    echo "Unsupported deployment environment: $environment"
    exit 2
    ;;
esac

shopt -s nullglob
artifacts=( $artifact_pattern )
if [[ ${#artifacts[@]} -eq 0 ]]; then
  echo "No deployable artifact matched: $artifact_pattern"
  exit 1
fi

if [[ ! -f docker-compose.yml ]]; then
  echo "docker-compose.yml was not found in the workspace."
  exit 1
fi

if docker compose version >/dev/null 2>&1; then
  compose_cmd=(docker compose)
elif command -v docker-compose >/dev/null 2>&1; then
  compose_cmd=(docker-compose)
else
  echo "Docker Compose is not available. Install Docker Compose on the Jenkins agent."
  exit 1
fi

echo "Deploying $environment to Docker"
echo "Artifact: ${artifacts[0]}"

"${compose_cmd[@]}" up -d --build --force-recreate rental-applications
"${compose_cmd[@]}" ps rental-applications
