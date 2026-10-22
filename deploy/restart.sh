#!/usr/bin/env bash
# 重启服务（不重新构建）。用法：./restart.sh [db|app|all]  默认 app
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "${SCRIPT_DIR}"
# shellcheck source=common.sh
source ./common.sh

TARGET="${1:-app}"
case "${TARGET}" in
  db|database) TARGET="db" ;;
  app|application) TARGET="app" ;;
  all) TARGET="all" ;;
  -h|--help)
    echo "用法: ./restart.sh [db|app|all]"
    exit 0
    ;;
  *)
    echo "[ERROR] 未知目标: ${TARGET}（可用 db|app|all）" >&2
    exit 1
    ;;
esac

if [[ ! -f .env ]]; then
  echo "[ERROR] 缺少 .env" >&2
  exit 1
fi

case "${TARGET}" in
  db)
    compose_db restart
    compose_db ps
    ;;
  app)
    compose_app restart
    compose_app ps
    ;;
  all)
    compose_db restart || true
    compose_app restart || true
    compose_db ps || true
    compose_app ps || true
    ;;
esac
echo "[INFO] 服务已重启（${TARGET}）"
