#!/usr/bin/env bash
set -euo pipefail

target=$DEPLOY_TARGET
stage=$DEPLOY_STAGING
compose_file=docker-compose.prod.yml
image_dir=.deploy-images
backend_image=${DEPLOY_BACKEND_IMAGE:-}
sidecar_image=${DEPLOY_SIDECAR_IMAGE:-}

# Each workflow uploads into its own staging directory.
if [[ $stage =~ ^/opt/sqb/\.deploy-staging/[0-9]+-[0-9]+$ ]]; then
  cleanup_staging() {
    rm -f -- "$stage/.github/scripts/deploy-production.sh" "$stage/docker-compose.prod.yml" || true
    rmdir -- "$stage/.github/scripts" "$stage/.github" "$stage" 2>/dev/null || true
  }
  trap cleanup_staging EXIT
fi

if [[ ! -f .env.prod ]]; then
  echo 'Missing /opt/sqb/.env.prod' >&2
  exit 1
fi
mkdir -p "$image_dir"

validate_image() {
  local service=$1 image=$2
  if [[ ! $image =~ ^ghcr\.io/[a-z0-9][a-z0-9-]*/sqb-$service@sha256:[a-f0-9]{64}$ ]]; then
    echo "Expected a GHCR digest for $service" >&2
    exit 1
  fi
}

case "$target" in
  backend)
    validate_image backend "$backend_image"
    docker pull "$backend_image"
    ;;
  ai-sidecar)
    validate_image ai-sidecar "$sidecar_image"
    docker pull "$sidecar_image"
    ;;
  stack)
    if [[ -n $backend_image ]]; then
      validate_image backend "$backend_image"
      docker pull "$backend_image"
    fi
    if [[ -n $sidecar_image ]]; then
      validate_image ai-sidecar "$sidecar_image"
      docker pull "$sidecar_image"
    fi
    ;;
  *) echo 'Unknown deployment target' >&2; exit 1 ;;
esac

# Separate app services may deploy concurrently. A stack update runs exclusively.
exec 9>"$image_dir/stack.lock"
if [[ $target == stack ]]; then
  flock -x -w 600 9
else
  flock -s -w 600 9
  exec 8>"$image_dir/$target.lock"
  flock -x -w 600 8
fi

if [[ $target == stack ]]; then
  compose_file="$stage/docker-compose.prod.yml"
elif [[ ! -f $compose_file ]]; then
  echo 'Run Deploy SQB Production Stack first to install docker-compose.prod.yml.' >&2
  exit 1
fi

compose() {
  local flags=() file
  for file in "$image_dir/backend.env" "$image_dir/ai-sidecar.env"; do
    if [[ -f $file ]]; then flags+=(--env-file "$file"); fi
  done
  docker compose -p sqb --project-directory "$PWD" --env-file .env.prod \
    "${flags[@]}" -f "$compose_file" "$@"
}

deployed_image() {
  local service=$1 container="sqb-$1" configured image_id digest
  if ! configured=$(docker container inspect --format '{{.Config.Image}}' "$container" 2>/dev/null); then
    return 0
  fi
  if [[ $configured == *@sha256:* || $configured == sqb-local/* ]]; then
    printf '%s\n' "$configured"
    return
  fi
  image_id=$(docker container inspect --format '{{.Image}}' "$container")
  # Resolve the digest from the running container's image ID, never from a mutable tag.
  while IFS= read -r digest; do
    if [[ $digest == "${configured%:*}@"* ]]; then
      printf '%s\n' "$digest"
      return
    fi
  done < <(docker image inspect --format '{{range .RepoDigests}}{{println .}}{{end}}' "$image_id")
  local pinned="sqb-local/$service:${image_id#sha256:}"
  docker image tag "$image_id" "$pinned"
  printf '%s\n' "$pinned"
}

last_ready_image() {
  local service=$1 variable=$2 file="$image_dir/$1.env" line
  if [[ -f $file ]]; then
    IFS= read -r line < "$file"
    printf '%s\n' "${line#"$variable="}"
  else
    deployed_image "$service"
  fi
}

record_image() {
  local service=$1 variable=$2 image=$3 pending
  pending=$(mktemp "$image_dir/$service.XXXXXX")
  printf '%s=%s\n' "$variable" "$image" > "$pending"
  mv -f -- "$pending" "$image_dir/$service.env"
}

record_existing_image() {
  local service=$1 variable=$2 image
  if [[ -f $image_dir/$service.env ]]; then return; fi
  image=$(deployed_image "$service")
  if [[ -n $image ]]; then record_image "$service" "$variable" "$image"; fi
}

wait_ready() {
  local service=$1 attempt
  for ((attempt = 0; attempt < 60; attempt++)); do
    if [[ $service == backend ]]; then
      if compose exec -T backend wget -q -T 3 -O /dev/null http://127.0.0.1:8080/api/v1/actuator/health/readiness >/dev/null 2>&1; then return 0; fi
    else
      if compose exec -T ai-sidecar curl -fsS --max-time 3 http://127.0.0.1:8000/health >/dev/null 2>&1; then return 0; fi
    fi
    sleep 5
  done
  compose ps "$service"
  echo "$service did not become ready." >&2
  return 1
}

case "$target" in
  backend)
    record_existing_image backend BACKEND_IMAGE
    export BACKEND_IMAGE="$backend_image"
    compose up -d --no-deps --pull never backend
    wait_ready backend
    record_image backend BACKEND_IMAGE "$backend_image"
    ;;
  ai-sidecar)
    record_existing_image ai-sidecar AI_SIDECAR_IMAGE
    export AI_SIDECAR_IMAGE="$sidecar_image"
    compose up -d --no-deps --pull never ai-sidecar
    wait_ready ai-sidecar
    record_image ai-sidecar AI_SIDECAR_IMAGE "$sidecar_image"
    ;;
  stack)
    record_existing_image backend BACKEND_IMAGE
    record_existing_image ai-sidecar AI_SIDECAR_IMAGE
    if [[ -z $backend_image ]]; then backend_image=$(last_ready_image backend BACKEND_IMAGE); fi
    if [[ -z $sidecar_image ]]; then sidecar_image=$(last_ready_image ai-sidecar AI_SIDECAR_IMAGE); fi
    if [[ -n $backend_image ]]; then export BACKEND_IMAGE="$backend_image"; fi
    if [[ -n $sidecar_image ]]; then export AI_SIDECAR_IMAGE="$sidecar_image"; fi

    available_services=$(compose config --services)
    services=$available_services
    if [[ -n ${DEPLOY_SERVICES:-} ]]; then
      if [[ ! $DEPLOY_SERVICES =~ ^[a-zA-Z0-9_-]+(\ [a-zA-Z0-9_-]+)*$ ]]; then
        echo 'Expected a space-separated list of Compose services.' >&2
        exit 1
      fi
      read -r -a selected <<< "$DEPLOY_SERVICES"
      services=$(printf '%s\n' "${selected[@]}")
      for service in "${selected[@]}"; do
        if ! grep -Fxq -- "$service" <<< "$available_services"; then
          echo "Unknown Compose service: $service" >&2
          exit 1
        fi
      done
    fi
    if [[ -n ${DEPLOY_BACKEND_IMAGE:-} ]] && ! grep -Fxq backend <<< "$services"; then
      echo 'Include backend when specifying a BE image.' >&2
      exit 1
    fi
    if [[ -n ${DEPLOY_SIDECAR_IMAGE:-} ]] && ! grep -Fxq ai-sidecar <<< "$services"; then
      echo 'Include ai-sidecar when specifying a sidecar image.' >&2
      exit 1
    fi
    infrastructure=()
    while IFS= read -r service; do
      case "$service" in
        backend|ai-sidecar|'') ;;
        *) infrastructure+=("$service") ;;
      esac
    done <<< "$services"
    if (( ${#infrastructure[@]} > 0 )); then
      compose up -d --pull missing "${infrastructure[@]}"
    fi
    if grep -Fxq ai-sidecar <<< "$services"; then
      compose up -d --no-deps --pull missing ai-sidecar
      wait_ready ai-sidecar
      sidecar_image=$(deployed_image ai-sidecar)
      [[ -n $sidecar_image ]]
      record_image ai-sidecar AI_SIDECAR_IMAGE "$sidecar_image"
    fi

    if grep -Fxq backend <<< "$services"; then
      if [[ -n ${DEPLOY_SERVICES:-} ]]; then
        compose up -d --no-deps --pull missing backend
      else
        compose up -d --pull missing backend
      fi
      wait_ready backend
      backend_image=$(deployed_image backend)
      [[ -n $backend_image ]]
      record_image backend BACKEND_IMAGE "$backend_image"
    fi

    cp -- "$compose_file" docker-compose.prod.yml.pending
    mv -f -- docker-compose.prod.yml.pending docker-compose.prod.yml
    ;;
esac

echo "Deployment complete: $target"
