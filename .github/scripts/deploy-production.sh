#!/usr/bin/env bash
set -euo pipefail

target=$DEPLOY_TARGET
stage=${DEPLOY_STAGING:-}
compose_file=docker-compose.prod.yml
image_dir=.deploy-images
tools_dir=.deploy-tools
env_snapshot=''
backend_image=${DEPLOY_BACKEND_IMAGE:-}
aisidecar_image=${DEPLOY_AISIDECAR_IMAGE:-}
legacy_aisidecar_service=ai-sidecar
legacy_stack=false

cleanup() {
  if [[ -n $env_snapshot ]]; then rm -f -- "$env_snapshot"; fi
  # Each workflow uploads into its own staging directory.
  if [[ $stage =~ ^/opt/sqb/\.deploy-staging/[0-9]+-[0-9]+$ ]]; then
    rm -f -- "$stage/.github/scripts/deploy-production.sh" "$stage/docker-compose.prod.yml" \
      "$stage/.github/deploy/sqb-production-env@.path" "$stage/.github/deploy/sqb-production-env@.service" || true
    rmdir -- "$stage/.github/scripts" "$stage/.github/deploy" "$stage/.github" "$stage" 2>/dev/null || true
  fi
}
trap cleanup EXIT

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
  aisidecar)
    validate_image aisidecar "$aisidecar_image"
    docker pull "$aisidecar_image"
    ;;
  stack)
    if [[ -n $backend_image ]]; then
      validate_image backend "$backend_image"
      docker pull "$backend_image"
    fi
    if [[ -n $aisidecar_image ]]; then
      validate_image aisidecar "$aisidecar_image"
      docker pull "$aisidecar_image"
    fi
    ;;
  env) ;;
  *) echo 'Unknown deployment target' >&2; exit 1 ;;
esac

# Separate app services may deploy concurrently. A stack update runs exclusively.
exec 9>"$image_dir/stack.lock"
if [[ $target == stack || $target == env ]]; then
  flock -x -w 600 9
else
  flock -s -w 600 9
  exec 8>"$image_dir/$target.lock"
  flock -x -w 600 8
fi

if [[ -f $compose_file ]] && grep -Eq "^[[:space:]]{2}$legacy_aisidecar_service:" "$compose_file"; then
  legacy_stack=true
  if [[ $target != stack ]]; then
    echo 'Deploy the complete production stack to install the AISidecar service rename first.' >&2
    exit 1
  fi
fi

if [[ $target == stack ]]; then
  compose_file="$stage/docker-compose.prod.yml"
elif [[ ! -f $compose_file ]]; then
  echo 'Run Deploy SQB Production Stack first to install docker-compose.prod.yml.' >&2
  exit 1
fi

env_snapshot=$(mktemp "$image_dir/env.XXXXXX")
env_source=.env.prod
if [[ $target == backend || $target == aisidecar || ${DEPLOY_USE_APPLIED_ENV:-false} == true ]]; then
  if [[ -f $image_dir/applied.env ]]; then env_source="$image_dir/applied.env"; fi
fi
cp -- "$env_source" "$env_snapshot"
compose_env_file=$env_snapshot

compose() {
  local flags=() file
  for file in "$image_dir/backend.env" "$image_dir/aisidecar.env"; do
    if [[ -f $file ]]; then flags+=(--env-file "$file"); fi
  done
  docker compose -p sqb --project-directory "$PWD" --env-file "$compose_env_file" \
    "${flags[@]}" -f "$compose_file" "$@"
}

validate_compose() {
  if ! compose config --quiet >/dev/null 2>&1; then
    echo 'Invalid production Compose or environment file.' >&2
    exit 1
  fi
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
  if [[ $legacy_stack == true && $service == aisidecar ]]; then
    service=$legacy_aisidecar_service
    variable=AI_SIDECAR_IMAGE
    file="$image_dir/$service.env"
  fi
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
      if compose exec -T "$service" curl -fsS --max-time 3 http://127.0.0.1:8000/health >/dev/null 2>&1; then return 0; fi
    fi
    sleep 5
  done
  compose ps "$service"
  echo "$service did not become ready." >&2
  return 1
}

save_applied_env() {
  local pending
  pending=$(mktemp "$image_dir/applied.XXXXXX")
  cp -- "$env_snapshot" "$pending"
  mv -f -- "$pending" "$image_dir/applied.env"
}

restore_apps() {
  local service
  for service in "$@"; do
    if compose up -d --no-deps --no-build --pull never "$service" && wait_ready "$service"; then
      echo "Restored the last applied configuration for $service."
    else
      echo "Could not restore the last applied configuration for $service." >&2
    fi
  done
}

changed_env_services() {
  local hashes previous_hashes service hash
  local -A previous=()
  if [[ ! -f $image_dir/applied.env ]]; then
    echo 'No applied environment snapshot. Deploy the complete production stack first.' >&2
    return 1
  fi

  local compose_env_file="$image_dir/applied.env"
  previous_hashes=$(compose config --hash '*' 2>/dev/null) || previous_hashes=''
  compose_env_file=$env_snapshot
  if ! hashes=$(compose config --hash '*' 2>/dev/null) || [[ -z $hashes || -z $previous_hashes ]]; then
    echo 'Could not compare production service configurations.' >&2
    return 1
  fi

  while read -r service hash; do
    previous[$service]=$hash
  done <<< "$previous_hashes"

  while read -r service hash; do
    if [[ $hash != "${previous[$service]:-}" ]]; then printf '%s\n' "$service"; fi
  done <<< "$hashes"
}

apply_runtime_env() {
  local affected service
  local -a attempted=()
  affected=$(changed_env_services) || return 1

  while IFS= read -r service; do
    case "$service" in
      backend|aisidecar|'') ;;
      *) echo "Environment changes affect $service. Apply them in a coordinated Stack deployment." >&2; return 1 ;;
    esac
  done <<< "$affected"

  for service in aisidecar backend; do
    if ! grep -Fxq "$service" <<< "$affected"; then continue; fi
    attempted+=("$service")
    if compose up -d --no-deps --no-build --pull never "$service" && wait_ready "$service"; then continue; fi

    echo "Failed to apply the new environment to $service." >&2
    compose_env_file="$image_dir/applied.env"
    restore_apps "${attempted[@]}"
    return 1
  done

  save_applied_env
}

install_deploy_tools() {
  install -d -- "$tools_dir"
  install -m 0644 -- "$stage/.github/deploy/sqb-production-env@.path" \
    "$stage/.github/deploy/sqb-production-env@.service" "$tools_dir/"
  install -m 0755 -- "$stage/.github/scripts/deploy-production.sh" "$tools_dir/deploy-production.sh.pending"
  mv -f -- "$tools_dir/deploy-production.sh.pending" "$tools_dir/deploy-production.sh"
}

case "$target" in
  backend)
    record_existing_image backend BACKEND_IMAGE
    export BACKEND_IMAGE="$backend_image"
    validate_compose
    compose up -d --no-deps --pull never backend
    wait_ready backend
    record_image backend BACKEND_IMAGE "$backend_image"
    ;;
  aisidecar)
    record_existing_image aisidecar AISIDECAR_IMAGE
    export AISIDECAR_IMAGE="$aisidecar_image"
    validate_compose
    compose up -d --no-deps --pull never aisidecar
    wait_ready aisidecar
    record_image aisidecar AISIDECAR_IMAGE "$aisidecar_image"
    ;;
  env)
    backend_image=$(last_ready_image backend BACKEND_IMAGE)
    aisidecar_image=$(last_ready_image aisidecar AISIDECAR_IMAGE)
    if [[ -z $backend_image || -z $aisidecar_image ]]; then
      echo 'No installed application images. Deploy the production stack first.' >&2
      exit 1
    fi
    export BACKEND_IMAGE="$backend_image" AISIDECAR_IMAGE="$aisidecar_image"
    validate_compose
    apply_runtime_env
    ;;
  stack)
    record_existing_image backend BACKEND_IMAGE
    record_existing_image aisidecar AISIDECAR_IMAGE
    previous_backend=$(last_ready_image backend BACKEND_IMAGE)
    previous_aisidecar=$(last_ready_image aisidecar AISIDECAR_IMAGE)
    backend_image=${backend_image:-$previous_backend}
    aisidecar_image=${aisidecar_image:-$previous_aisidecar}
    if [[ -n $backend_image ]]; then export BACKEND_IMAGE="$backend_image"; fi
    if [[ -n $aisidecar_image ]]; then export AISIDECAR_IMAGE="$aisidecar_image"; fi
    validate_compose

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
    if [[ -n ${DEPLOY_AISIDECAR_IMAGE:-} ]] && ! grep -Fxq aisidecar <<< "$services"; then
      echo 'Include aisidecar when specifying an AISidecar image.' >&2
      exit 1
    fi
    if [[ $legacy_stack == true && ( -z ${DEPLOY_BACKEND_IMAGE:-} || -z ${DEPLOY_AISIDECAR_IMAGE:-} ) ]]; then
      echo 'The AISidecar rename requires deploying both services with their new image digests.' >&2
      exit 1
    fi
    if [[ -n ${DEPLOY_SERVICES:-} && $env_source == .env.prod && -f $image_dir/applied.env ]]; then
      affected=$(changed_env_services)
      while IFS= read -r service; do
        if [[ -n $service ]] && ! grep -Fxq "$service" <<< "$services"; then
          echo "Include $service when applying the pending environment changes." >&2
          exit 1
        fi
      done <<< "$affected"
    fi
    infrastructure=()
    while IFS= read -r service; do
      case "$service" in
        backend|aisidecar|'') ;;
        *) infrastructure+=("$service") ;;
      esac
    done <<< "$services"
    if (( ${#infrastructure[@]} > 0 )); then
      compose up -d --no-build --pull missing "${infrastructure[@]}"
    fi
    attempted_apps=()
    for service in aisidecar backend; do
      if ! grep -Fxq "$service" <<< "$services"; then continue; fi
      attempted_apps+=("$service")
      dependency_flags=()
      if [[ $service == aisidecar || -n ${DEPLOY_SERVICES:-} ]]; then dependency_flags+=(--no-deps); fi
      if ! compose up -d --no-build --pull missing "${dependency_flags[@]}" "$service" || ! wait_ready "$service"; then
        if [[ -f docker-compose.prod.yml && -n $previous_backend && -n $previous_aisidecar ]]; then
          compose_file=docker-compose.prod.yml
          if [[ -f $image_dir/applied.env ]]; then compose_env_file="$image_dir/applied.env"; fi
          export BACKEND_IMAGE="$previous_backend" AISIDECAR_IMAGE="$previous_aisidecar"
          if [[ $legacy_stack == true ]]; then
            export AI_SIDECAR_IMAGE="$previous_aisidecar"
            attempted_apps=("$legacy_aisidecar_service" "${attempted_apps[@]:1}")
          fi
          restore_apps "${attempted_apps[@]}"
        fi
        exit 1
      fi
    done

    if grep -Fxq aisidecar <<< "$services"; then
      aisidecar_image=$(deployed_image aisidecar)
      [[ -n $aisidecar_image ]]
      record_image aisidecar AISIDECAR_IMAGE "$aisidecar_image"
    fi
    if grep -Fxq backend <<< "$services"; then
      backend_image=$(deployed_image backend)
      [[ -n $backend_image ]]
      record_image backend BACKEND_IMAGE "$backend_image"
    fi

    cp -- "$compose_file" docker-compose.prod.yml.pending
    mv -f -- docker-compose.prod.yml.pending docker-compose.prod.yml
    install_deploy_tools
    if [[ -z ${DEPLOY_SERVICES:-} || -f $image_dir/applied.env ]]; then save_applied_env; fi
    ;;
esac

echo "Deployment complete: $target"
