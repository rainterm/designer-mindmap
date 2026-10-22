#!/usr/bin/env bash
# =====================================================================
# 使用本地 JDK / Maven / Node 打包，再构建 Docker 镜像并部署应用
# 用法：./deploy-app.sh [--rebuild] [--no-cache] [--pull] [--skip-build]
# 前置：本机已安装 JDK 25、Maven、Node.js；数据库已就绪
# =====================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
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

REBUILD=0
NO_CACHE=0
PULL=0
SKIP_BUILD=0

for arg in "$@"; do
  case "${arg}" in
    --rebuild)    REBUILD=1 ;;
    --no-cache)   NO_CACHE=1 ;;
    --pull)       PULL=1 ;;
    --skip-build) SKIP_BUILD=1 ;;
    -h|--help)
      cat <<'EOF'
用法: ./deploy-app.sh [选项]

  --rebuild     强制重新构建 Docker 镜像
  --no-cache    Docker 构建不使用缓存
  --pull        拉取基础镜像最新版本
  --skip-build  跳过本地 Maven/Node 打包（使用已有产物）
  -h, --help    显示帮助

流程：
  1. 仓库根目录 mvn -pl designer-server -am package 打后端胖包
  2. 本地 pnpm run build 打 admin-web 静态资源
  3. Docker 仅打包运行镜像并启动（Server / Web）
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

# 确保 Maven 使用 JDK 25（避免 PATH 上 java 是 25，但 JAVA_HOME/Maven 仍指向旧版）
ensure_java25() {
  local home=""
  if [[ "$(uname -s)" == "Darwin" ]] && [[ -x /usr/libexec/java_home ]]; then
    home="$(/usr/libexec/java_home -v 25 2>/dev/null || true)"
  fi
  if [[ -z "${home}" ]]; then
    # Linux / 其它：从当前 java 反推
    local java_bin
    java_bin="$(command -v java || true)"
    if [[ -n "${java_bin}" ]]; then
      java_bin="$(readlink -f "${java_bin}" 2>/dev/null || echo "${java_bin}")"
      home="$(cd "$(dirname "${java_bin}")/.." && pwd)"
    fi
  fi
  if [[ -z "${home}" || ! -x "${home}/bin/java" ]]; then
    err "未找到 JDK 25，请先执行：./install-env.sh"
    exit 1
  fi
  if ! "${home}/bin/java" -version 2>&1 | grep -Eq '"25(\.|$)|version 25'; then
    err "JAVA_HOME 不是 JDK 25：${home}"
    err "$("${home}/bin/java" -version 2>&1 | head -n1)"
    exit 1
  fi
  export JAVA_HOME="${home}"
  export PATH="${JAVA_HOME}/bin:${PATH}"
  log "JAVA_HOME=${JAVA_HOME}"
}

need_local_toolchain() {
  local missing=0
  for cmd in java mvn node pnpm; do
    if ! command -v "${cmd}" >/dev/null 2>&1; then
      err "未找到 ${cmd}，请先执行：./install-env.sh（Linux 需 sudo）"
      missing=1
    fi
  done
  if [[ "${missing}" -ne 0 ]]; then
    err "当前环境：java=$(command -v java || echo 无) mvn=$(command -v mvn || echo 无) node=$(command -v node || echo 无) pnpm=$(command -v pnpm || echo 无)"
    exit 1
  fi
  ensure_java25
  log "本地工具链："
  log "  $(java -version 2>&1 | head -n1)"
  log "  $(mvn -v 2>/dev/null | head -n3 | tr '\n' ' ')"
  log "  Node $(node -v) / pnpm $(pnpm -v)"
}

# 构建单个前端工程；$1=源码目录 $2=说明名
build_frontend() {
  local src_dir="$1"
  local name="$2"
  local dist="${src_dir}/dist"

  log ">>> 本地打包前端 ${name}（Node / pnpm / Vite）"
  (
    cd "${src_dir}"
    if [[ -f pnpm-lock.yaml ]]; then
      pnpm install --frozen-lockfile
    else
      pnpm install
    fi
    # 生产同源 /admin-api、/app-api，由 Nginx 反代
    VITE_API_BASE= pnpm run build
  )
  if [[ ! -d "${dist}" ]] || [[ -z "$(ls -A "${dist}" 2>/dev/null || true)" ]]; then
    err "未找到前端产物：${dist}"
    exit 1
  fi
}

package_locally() {
  local root_dir="${ROOT_DIR}"
  local server_dir="${ROOT_DIR}/designer-server"
  local admin_dir="${ROOT_DIR}/designer-ui/admin-web"
  local jar="${server_dir}/target/designer-server.jar"

  log ">>> 本地打包后端（Maven 多模块）"
  (
    cd "${root_dir}"
    # 从父工程构建 server 及其依赖模块，产物仍在 designer-server/target/
    JAVA_HOME="${JAVA_HOME}" PATH="${JAVA_HOME}/bin:${PATH}" \
      mvn -pl designer-server -am -DskipTests clean package
  )
  if [[ ! -f "${jar}" ]]; then
    err "未找到胖包：${jar}"
    exit 1
  fi

  build_frontend "${admin_dir}" "admin-web"

  log ">>> 同步产物到 deploy/build/"
  rm -rf "${SCRIPT_DIR}/build/server" "${SCRIPT_DIR}/build/web"
  mkdir -p "${SCRIPT_DIR}/build/server" "${SCRIPT_DIR}/build/web"
  cp "${jar}" "${SCRIPT_DIR}/build/server/designer-server.jar"
  cp -R "${admin_dir}/dist" "${SCRIPT_DIR}/build/web/dist"
  log "本地打包完成"
}

DB_HOST="$(env_get DB_HOST)"; DB_HOST="${DB_HOST:-host.docker.internal}"
DB_PORT_VAL="$(env_get DB_PORT)"; DB_PORT_VAL="${DB_PORT_VAL:-3306}"
DB_PASSWORD="$(env_get DB_PASSWORD)"
REDIS_HOST_VAL="$(env_get REDIS_HOST)"; REDIS_HOST_VAL="${REDIS_HOST_VAL:-host.docker.internal}"
REDIS_PORT_VAL="$(env_get REDIS_PORT)"; REDIS_PORT_VAL="${REDIS_PORT_VAL:-6379}"
REDIS_PASSWORD="$(env_get REDIS_PASSWORD)"
APP_PORT="$(env_get APP_PORT)"; APP_PORT="${APP_PORT:-80}"
SERVER_PORT="$(env_get SERVER_PORT)"; SERVER_PORT="${SERVER_PORT:-8080}"

if [[ -z "${DB_PASSWORD}" ]]; then
  err "请在 .env 中配置 DB_PASSWORD（连接数据库的密码）"
  exit 1
fi
if [[ -z "${REDIS_PASSWORD}" ]]; then
  err "请在 .env 中配置 REDIS_PASSWORD"
  exit 1
fi
if [[ "${DB_PASSWORD}" == "ChangeMe_Root_Passw0rd" ]] \
  || [[ "${REDIS_PASSWORD}" == "ChangeMe_Redis_Passw0rd" ]]; then
  warn "检测到示例密码，生产环境强烈建议修改 .env"
fi

if [[ "${SKIP_BUILD}" -eq 1 ]]; then
  warn "已跳过本地打包，使用 deploy/build/ 现有产物"
  if [[ ! -f "${SCRIPT_DIR}/build/server/designer-server.jar" ]] \
    || [[ ! -d "${SCRIPT_DIR}/build/web/dist" ]]; then
    err "缺少 deploy/build 产物，请去掉 --skip-build 重新打包"
    exit 1
  fi
else
  need_local_toolchain
  package_locally
fi

log "构建并启动应用容器（Server / Web）..."
log "  数据库 : ${DB_HOST}:${DB_PORT_VAL}"
log "  Redis  : ${REDIS_HOST_VAL}:${REDIS_PORT_VAL}"

UP_ARGS=(up -d)
[[ "${PULL}" -eq 1 ]] && UP_ARGS+=(--pull always)

if [[ "${REBUILD}" -eq 1 || "${NO_CACHE}" -eq 1 ]]; then
  BUILD_ARGS=(build)
  [[ "${NO_CACHE}" -eq 1 ]] && BUILD_ARGS+=(--no-cache)
  compose_app "${BUILD_ARGS[@]}"
  UP_ARGS+=(--force-recreate)
else
  UP_ARGS+=(--build)
fi

compose_app "${UP_ARGS[@]}"

log "等待应用就绪..."
ATTEMPTS=60
for ((i = 1; i <= ATTEMPTS; i++)); do
  if curl -fsS "http://127.0.0.1:${APP_PORT}/admin-api/system/health" >/dev/null 2>&1 \
    || curl -fsS "http://127.0.0.1:${SERVER_PORT}/admin-api/system/health" >/dev/null 2>&1; then
    break
  fi
  if [[ "${i}" -eq "${ATTEMPTS}" ]]; then
    warn "健康检查超时，请执行：./logs.sh server"
  else
    sleep 3
  fi
done

echo
compose_app ps
echo
log "应用部署完成"
log "  管理后台 : http://<服务器IP>:${APP_PORT}"
log "  后端直连 : http://<服务器IP>:${SERVER_PORT}"
log "  默认账号 : admin / admin123"
log "  查看日志 : ./logs.sh server"
