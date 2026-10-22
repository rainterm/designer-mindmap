#!/usr/bin/env bash
# =====================================================================
# 部署数据库（MySQL + Redis），不构建应用镜像
# 用法：./deploy-db.sh [--pull]
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
用法: ./deploy-db.sh [选项]

  --pull      拉取 MySQL / Redis 最新镜像
  -h, --help  显示帮助

仅启动 MySQL + Redis（不会自动建表）。
首次使用或需要重置时，请在仓库根目录手动初始化：
  bash sql/init/mysql/00-run-init.sh
应用请另执行：./deploy-app.sh
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

MYSQL_ROOT_PASSWORD="$(env_get MYSQL_ROOT_PASSWORD)"
REDIS_PASSWORD="$(env_get REDIS_PASSWORD)"
MYSQL_PORT_VAL="$(env_get MYSQL_PORT)"; MYSQL_PORT_VAL="${MYSQL_PORT_VAL:-3306}"
REDIS_PORT_VAL="$(env_get REDIS_PORT)"; REDIS_PORT_VAL="${REDIS_PORT_VAL:-6379}"

if [[ "${MYSQL_ROOT_PASSWORD}" == "ChangeMe_Root_Passw0rd" ]] \
  || [[ "${REDIS_PASSWORD}" == "ChangeMe_Redis_Passw0rd" ]]; then
  warn "检测到示例密码，生产环境强烈建议修改 .env"
fi

log "启动 MySQL / Redis ..."
UP_ARGS=(up -d)
[[ "${PULL}" -eq 1 ]] && UP_ARGS+=(--pull always)
compose_db "${UP_ARGS[@]}"

log "等待 MySQL 就绪..."
ATTEMPTS=40
for ((i = 1; i <= ATTEMPTS; i++)); do
  if compose_db exec -T mysql \
      mysqladmin ping -h 127.0.0.1 -uroot -p"${MYSQL_ROOT_PASSWORD}" --silent \
      >/dev/null 2>&1; then
    break
  fi
  if [[ "${i}" -eq "${ATTEMPTS}" ]]; then
    warn "MySQL 健康检查超时，请执行：./logs.sh mysql"
  else
    sleep 3
  fi
done

echo
compose_db ps
echo
log "数据库部署完成"
log "  MySQL  : 127.0.0.1:${MYSQL_PORT_VAL}"
log "  Redis  : 127.0.0.1:${REDIS_PORT_VAL}"
log "  建表   : 在仓库根目录执行 bash sql/init/mysql/00-run-init.sh（首次或重置时）"
log "  下一步 : ./deploy-app.sh"
