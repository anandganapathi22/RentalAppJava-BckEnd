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

echo "Deployment hook for $environment"
echo "Artifact: ${artifacts[0]}"
echo "Add the real $environment deployment command here when the target platform is known."
