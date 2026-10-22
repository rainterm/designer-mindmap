#!/usr/bin/env bash
# 查看日志。用法：./logs.sh [mysql|redis|server|web]
# 不传参数时跟踪应用（server+web）日志
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "${SCRIPT_DIR}"
# shellcheck source=common.sh
source ./common.sh

if [[ ! -f .env ]]; then
  echo "[ERROR] 缺少 .env" >&2
  exit 1
fi

SERVICE="${1:-}"
case "${SERVICE}" in
  mysql|redis)
    compose_db logs -f --tail=200 "${SERVICE}"
    ;;
  server|web)
    compose_app logs -f --tail=200 "${SERVICE}"
    ;;
  "")
    compose_app logs -f --tail=100
    ;;
  -h|--help)
    echo "用法: ./logs.sh [mysql|redis|server|web]"
    exit 0
    ;;
  *)
    echo "[ERROR] 未知服务: ${SERVICE}（可用 mysql|redis|server|web）" >&2
    exit 1
    ;;
esac
