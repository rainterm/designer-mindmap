#!/usr/bin/env bash
# =====================================================================
# 部署本地对象存储 MinIO（S3 兼容，供文件存储的 S3 Provider 使用）
# 用法：./deploy-minio.sh [--pull]
# =====================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "${SCRIPT_DIR}"
# shellcheck source=common.sh
source ./common.sh

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'
log()  { echo -e "${GREEN}[INFO]${NC} $*"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $*"; }
err()  { echo -e "${RED}[ERROR]${NC} $*" >&2; }

PULL=0
for arg in "$@"; do
  case "${arg}" in
    --pull) PULL=1 ;;
    -h|--help)
      cat <<'EOF'
用法: ./deploy-minio.sh [选项]

  --pull      拉取最新镜像
  -h, --help  显示帮助

启动后可在「系统管理 → 存储配置」中填写：
  endpoint   http://127.0.0.1:9000
  accessKey  .env 中的 MINIO_ROOT_USER
  secretKey  .env 中的 MINIO_ROOT_PASSWORD
EOF
      exit 0
      ;;
    *)
      err "未知参数: ${arg}"
      exit 1
      ;;
  esac
done

need_docker
ensure_env_file

API_PORT_VAL="$(env_get MINIO_API_PORT)"; API_PORT_VAL="${API_PORT_VAL:-9000}"
CONSOLE_PORT_VAL="$(env_get MINIO_CONSOLE_PORT)"; CONSOLE_PORT_VAL="${CONSOLE_PORT_VAL:-9001}"

log "启动 MinIO ..."
UP_ARGS=(up -d)
[[ "${PULL}" -eq 1 ]] && UP_ARGS+=(--pull always)
compose_minio "${UP_ARGS[@]}"

log "等待 MinIO 就绪..."
ATTEMPTS=40
for ((i = 1; i <= ATTEMPTS; i++)); do
  if curl -sf "http://127.0.0.1:${API_PORT_VAL}/minio/health/live" >/dev/null 2>&1; then
    break
  fi
  if [[ "${i}" -eq "${ATTEMPTS}" ]]; then
    warn "MinIO 健康检查超时，请执行：docker logs designer-admin-minio"
  else
    sleep 3
  fi
done

echo
compose_minio ps
echo
log "MinIO 部署完成"
log "  S3 API  : http://127.0.0.1:${API_PORT_VAL}"
log "  Console : http://127.0.0.1:${CONSOLE_PORT_VAL}"
