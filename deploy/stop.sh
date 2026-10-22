#!/usr/bin/env bash
# 停止服务。用法：./stop.sh [db|app|minio|all]   默认 all
# 保留数据卷；清空数据请加 --volumes（仅对所选目标生效）
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "${SCRIPT_DIR}"
# shellcheck source=common.sh
source ./common.sh

TARGET="all"
WITH_VOLUMES=0
for arg in "$@"; do
  case "${arg}" in
    db|database) TARGET="db" ;;
    app|application) TARGET="app" ;;
    minio) TARGET="minio" ;;
    all) TARGET="all" ;;
    -v|--volumes) WITH_VOLUMES=1 ;;
    -h|--help)
      echo "用法: ./stop.sh [db|app|minio|all] [--volumes]"
      exit 0
      ;;
    *)
      echo "[ERROR] 未知参数: ${arg}" >&2
      exit 1
      ;;
  esac
done

if [[ ! -f .env ]]; then
  echo "[ERROR] 缺少 .env" >&2
  exit 1
fi

DOWN_ARGS=(down)
[[ "${WITH_VOLUMES}" -eq 1 ]] && DOWN_ARGS+=(-v)

case "${TARGET}" in
  db)
    compose_db "${DOWN_ARGS[@]}"
    echo "[INFO] 已停止数据库（MySQL / Redis）"
    ;;
  app)
    compose_app "${DOWN_ARGS[@]}"
    echo "[INFO] 已停止应用（Server / Web）"
    ;;
  minio)
    compose_minio "${DOWN_ARGS[@]}"
    echo "[INFO] 已停止 MinIO"
    ;;
  all)
    compose_app "${DOWN_ARGS[@]}" || true
    compose_db "${DOWN_ARGS[@]}" || true
    compose_minio "${DOWN_ARGS[@]}" || true
    echo "[INFO] 已停止全部服务（数据卷默认保留）"
    ;;
esac
[[ "${WITH_VOLUMES}" -eq 1 ]] && echo "[INFO] 已同时删除对应数据卷"
