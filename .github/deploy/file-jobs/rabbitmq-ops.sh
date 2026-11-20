#!/usr/bin/env bash
# Run on the Linux VPS. This wrapper manages only the independent broker project.
set -euo pipefail

script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
repo_root=$(cd -- "$script_dir/../../.." && pwd)
env_file=${SQB_RABBITMQ_ENV_FILE:-"$repo_root/.env.rabbitmq"}
action=${1:-check}

case "$action" in
  check|start|status|stop) ;;
  *) echo 'Usage: rabbitmq-ops.sh [check|start|status|stop]' >&2; exit 2 ;;
esac

command -v docker >/dev/null
command -v python3 >/dev/null
command -v flock >/dev/null
[[ -f $env_file ]] || { echo 'Missing private .env.rabbitmq; see the runbook.' >&2; exit 1; }
mode=$(stat -c '%a' -- "$env_file")
[[ $mode == 600 || $mode == 400 ]] || { echo 'Private env file must have mode 600 or 400.' >&2; exit 1; }

compose() {
  docker compose -p sqb-file-jobs --env-file "$env_file" \
    -f "$script_dir/docker-compose.rabbitmq.yml" "$@"
}

compose config --quiet
# Parse resolved config in memory; never print it (it contains the bootstrap password).
network_name=$(compose config --format json | python3 -c '
import json, sys
cfg = json.load(sys.stdin)
password = cfg["services"]["rabbitmq"]["environment"]["RABBITMQ_DEFAULT_PASS"]
if password == "REPLACE_WITH_A_UNIQUE_RANDOM_PASSWORD" or len(password) < 24:
    sys.exit("Set a unique password of at least 24 characters before using this wrapper.")
print(cfg["networks"]["sqb-network"]["name"])
')
docker info >/dev/null
docker network inspect "$network_name" >/dev/null

if [[ $action == check ]]; then
  echo 'Compose, env permissions, Docker Engine and existing application network checked.'
  echo 'Broker startup, credentials and memory still require start/status verification.'
  exit 0
fi

# Cooperate with the existing production deploy lock; do not change other services.
mkdir -p -- "$repo_root/.deploy-images"
exec 9>"$repo_root/.deploy-images/stack.lock"
flock -x -w 600 9

case "$action" in
  start)
    compose pull rabbitmq
    compose up -d --no-deps --wait --wait-timeout 180 rabbitmq
    compose exec -T rabbitmq rabbitmq-diagnostics -q check_local_alarms
    echo 'Broker started. Existing APIs have not been converted into background jobs.'
    ;;
  stop)
    compose stop rabbitmq
    echo 'Broker stopped; persistent data retained. Do not stop an active job system without its drain procedure.'
    ;;
  status)
    compose ps rabbitmq
    compose exec -T rabbitmq rabbitmq-diagnostics -q check_running
    compose exec -T rabbitmq rabbitmq-diagnostics -q check_local_alarms
    compose exec -T rabbitmq rabbitmqctl list_user_permissions sqb_file_jobs
    compose exec -T rabbitmq rabbitmqctl list_queues -p sqb-file-jobs name type messages_ready messages_unacknowledged consumers
    container_id=$(compose ps -q rabbitmq)
    [[ -z $container_id ]] || docker stats --no-stream "$container_id"
    ;;
esac
