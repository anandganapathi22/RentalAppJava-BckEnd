#!/usr/bin/env bash
set -euo pipefail

state_file="${1:-}"
artifact_pattern="${2:-}"

if [[ -z "$state_file" || -z "$artifact_pattern" ]]; then
  echo "Usage: $0 <promotion-state-file> <artifact-pattern>"
  exit 2
fi

current_environment="none"
if [[ -f "$state_file" ]]; then
  current_environment="$(tr -d '[:space:]' < "$state_file")"
fi

case "$current_environment" in
  none|"")
    echo "No dev deployment state found. Run PIPELINE_FLOW=main first."
    exit 1
    ;;
  dev)
    next_environment="stage"
    ;;
  stage)
    next_environment="prod"
    ;;
  prod)
    echo "Current release is already promoted to prod."
    exit 0
    ;;
  *)
    echo "Unknown promotion state: $current_environment"
    exit 1
    ;;
esac

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
"$script_dir/deploy.sh" "$next_environment" "$artifact_pattern"

mkdir -p "$(dirname "$state_file")"
printf "%s\n" "$next_environment" > "$state_file"
echo "Promoted release from $current_environment to $next_environment"
