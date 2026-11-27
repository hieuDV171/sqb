#!/usr/bin/env bash
set -euo pipefail

before=$1
after=$2
backend=false
aisidecar=false
stack=false

if git cat-file -e "$before^{commit}" 2>/dev/null; then
  paths=$(mktemp)
  trap 'rm -f -- "$paths"' EXIT
  git diff --no-renames --name-only -z "$before" "$after" > "$paths"
  while IFS= read -r -d '' path; do
    case "$path" in
      backend/*) backend=true ;;
      aisidecar/*) aisidecar=true ;;
      .github/workflows/build-production-image.yml) backend=true; aisidecar=true ;;
      docker-compose.prod.yml|.github/workflows/deploy-stack.yml|.github/workflows/deploy-production.yml|.github/scripts/deploy-production.sh|.github/scripts/production-changes.sh|.github/deploy/*) stack=true ;;
    esac
  done < "$paths"
else
  echo '::warning::Previous revision unavailable. Applying the complete production revision.'
  backend=true
  aisidecar=true
  stack=true
fi

printf 'backend=%s\naisidecar=%s\nstack=%s\n' "$backend" "$aisidecar" "$stack" >> "$GITHUB_OUTPUT"
