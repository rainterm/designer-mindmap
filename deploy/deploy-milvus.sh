#!/usr/bin/env bash
# =====================================================================
# 部署本地 Milvus Standalone（etcd + 内部 MinIO + milvus）
# 用法：./deploy-milvus.sh [--pull]
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
用法: ./deploy-milvus.sh [选项]

  --pull      拉取最新镜像
  -h, --help  显示帮助

启动后应用默认连接：http://127.0.0.1:19530
（app.yml: designer.agent.milvus.uri）
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

MILVUS_PORT_VAL="$(env_get MILVUS_PORT)"; MILVUS_PORT_VAL="${MILVUS_PORT_VAL:-19530}"
MILVUS_HEALTH_PORT_VAL="$(env_get MILVUS_HEALTH_PORT)"; MILVUS_HEALTH_PORT_VAL="${MILVUS_HEALTH_PORT_VAL:-9091}"

log "启动 Milvus Standalone ..."
UP_ARGS=(up -d)
[[ "${PULL}" -eq 1 ]] && UP_ARGS+=(--pull always)
compose_milvus "${UP_ARGS[@]}"

log "等待 Milvus 就绪..."
ATTEMPTS=60
for ((i = 1; i <= ATTEMPTS; i++)); do
  if curl -sf "http://127.0.0.1:${MILVUS_HEALTH_PORT_VAL}/healthz" >/dev/null 2>&1; then
    break
  fi
  if [[ "${i}" -eq "${ATTEMPTS}" ]]; then
    warn "Milvus 健康检查超时，请执行：docker logs designer-admin-milvus"
  else
    sleep 3
  fi
done

echo
compose_milvus ps
echo
log "Milvus 部署完成"
log "  gRPC / SDK : 127.0.0.1:${MILVUS_PORT_VAL}"
log "  Health     : http://127.0.0.1:${MILVUS_HEALTH_PORT_VAL}/healthz"
log "  应用配置   : designer.agent.milvus.uri=http://127.0.0.1:${MILVUS_PORT_VAL}"
