#!/usr/bin/env bash
# =====================================================================
# 部署环境检查与安装：Docker / JDK 25 / Maven / Node.js
# 缺失则自动安装（支持 macOS Homebrew、Ubuntu/Debian、RHEL 系）
# 用法：
#   ./install-env.sh              # 检查，缺什么装什么（Linux 装系统包需 sudo）
#   ./install-env.sh --check      # 仅检查，不安装
# =====================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "${SCRIPT_DIR}"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'
log()  { echo -e "${GREEN}[INFO]${NC} $*"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $*"; }
err()  { echo -e "${RED}[ERROR]${NC} $*" >&2; }
ok()   { echo -e "${GREEN}[ OK ]${NC} $*"; }
fail() { echo -e "${RED}[MISS]${NC} $*"; }

CHECK_ONLY=0
for arg in "$@"; do
  case "${arg}" in
    --check) CHECK_ONLY=1 ;;
    -h|--help)
      cat <<'EOF'
用法: ./install-env.sh [选项]

  （默认）  检查 Docker / JDK / Maven / Node，缺失则安装
  --check   仅检查，不安装
  -h, --help

Linux 安装系统级组件时请使用：sudo ./install-env.sh
macOS 建议先安装 Homebrew，脚本会通过 brew 安装缺失工具。
EOF
      exit 0
      ;;
    *)
      err "未知参数: ${arg}"
      exit 1
      ;;
  esac
done

OS_FAMILY=""
OS_ID=""
detect_os() {
  case "$(uname -s)" in
    Darwin)
      OS_FAMILY="macos"
      OS_ID="macos"
      log "检测到系统：macOS $(sw_vers -productVersion 2>/dev/null || true)"
      ;;
    Linux)
      OS_FAMILY="linux"
      if [[ -f /etc/os-release ]]; then
        # shellcheck disable=SC1091
        . /etc/os-release
        OS_ID="${ID:-}"
        log "检测到系统：${PRETTY_NAME:-$OS_ID}"
      else
        err "无法识别 Linux 发行版"
        exit 1
      fi
      ;;
    *)
      err "不支持的操作系统：$(uname -s)"
      exit 1
      ;;
  esac
}

have_cmd() { command -v "$1" >/dev/null 2>&1; }

# ---- 检测 ----
check_docker() {
  if have_cmd docker && docker compose version >/dev/null 2>&1; then
    ok "Docker: $(docker -v 2>/dev/null | head -n1)"
    ok "Compose: $(docker compose version 2>/dev/null | head -n1)"
    return 0
  fi
  fail "Docker / Compose 未安装或不可用"
  return 1
}

check_jdk() {
  if ! have_cmd java; then
    fail "JDK 未安装（需要 JDK 25）"
    return 1
  fi
  local ver
  ver="$(java -version 2>&1 | head -n1 || true)"
  if java -version 2>&1 | grep -Eq '"25(\.|$)|version 25'; then
    ok "JDK: ${ver}"
    return 0
  fi
  # 宽松：只要有 java，提示版本；部署要求 25
  if java -version 2>&1 | grep -Eq '"2[5-9](\.|$)|version 2[5-9]'; then
    ok "JDK: ${ver}"
    return 0
  fi
  fail "JDK 版本偏低（需要 25+）：${ver}"
  return 1
}

check_maven() {
  if have_cmd mvn; then
    ok "Maven: $(mvn -v 2>/dev/null | head -n1)"
    return 0
  fi
  fail "Maven 未安装"
  return 1
}

check_node() {
  if ! have_cmd node; then
    fail "Node.js 未安装"
    return 1
  fi
  local major
  major="$(node -v | sed 's/^v//' | cut -d. -f1)"
  if [[ "${major}" -lt 18 ]]; then
    fail "Node 版本偏低（需要 18+）：$(node -v)"
    return 1
  fi
  if ! have_cmd pnpm; then
    fail "pnpm 未安装（可执行：corepack enable pnpm，或 npm i -g pnpm）"
    return 1
  fi
  ok "Node: $(node -v) / pnpm $(pnpm -v)"
  return 0
}

# ---- 安装辅助 ----
require_root_linux() {
  if [[ "${OS_FAMILY}" != "linux" ]]; then
    return 0
  fi
  if [[ "${EUID}" -ne 0 ]]; then
    err "Linux 下安装系统组件请使用：sudo $0"
    exit 1
  fi
}

ensure_brew() {
  if have_cmd brew; then
    return 0
  fi
  err "未找到 Homebrew。请先安装：https://brew.sh  然后重新执行本脚本"
  exit 1
}

run_as_invoke_user() {
  # root 下用原用户执行 brew，避免 brew 拒绝 root
  if [[ "${EUID}" -eq 0 && -n "${SUDO_USER:-}" && "${SUDO_USER}" != "root" ]]; then
    sudo -u "${SUDO_USER}" -H "$@"
  else
    "$@"
  fi
}

# ---- Docker ----
install_docker() {
  log "正在安装 Docker ..."
  case "${OS_FAMILY}" in
    macos)
      ensure_brew
      if brew list --cask docker >/dev/null 2>&1 || [[ -d /Applications/Docker.app ]]; then
        warn "检测到 Docker Desktop，请确认已启动 Docker"
      else
        run_as_invoke_user brew install --cask docker
        warn "已安装 Docker Desktop，请从「应用程序」启动后再继续部署"
      fi
      ;;
    linux)
      require_root_linux
      case "${OS_ID}" in
        ubuntu|debian)
          apt-get update -y
          apt-get install -y ca-certificates curl gnupg lsb-release
          install -m 0755 -d /etc/apt/keyrings
          if [[ ! -f /etc/apt/keyrings/docker.gpg ]]; then
            curl -fsSL "https://download.docker.com/linux/${OS_ID}/gpg" \
              | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
            chmod a+r /etc/apt/keyrings/docker.gpg
          fi
          ARCH="$(dpkg --print-architecture)"
          CODENAME="$(. /etc/os-release && echo "${VERSION_CODENAME}")"
          echo "deb [arch=${ARCH} signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/${OS_ID} ${CODENAME} stable" \
            > /etc/apt/sources.list.d/docker.list
          apt-get update -y
          apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
          ;;
        centos|rhel|rocky|almalinux|ol|fedora)
          if have_cmd dnf; then
            dnf -y install dnf-plugins-core
            dnf config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo || true
            dnf -y install docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
          else
            yum install -y yum-utils
            yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
            yum install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
          fi
          ;;
        *)
          err "暂不支持自动安装 Docker 的发行版：${OS_ID}"
          exit 1
          ;;
      esac
      systemctl enable docker
      systemctl start docker
      if [[ -n "${SUDO_USER:-}" && "${SUDO_USER}" != "root" ]]; then
        usermod -aG docker "${SUDO_USER}" || true
        warn "已将 ${SUDO_USER} 加入 docker 组，重新登录后生效"
      fi
      ;;
  esac
}

# ---- JDK 25 (Temurin) ----
install_jdk() {
  log "正在安装 JDK 25 (Temurin) ..."
  case "${OS_FAMILY}" in
    macos)
      ensure_brew
      run_as_invoke_user brew install --cask temurin@25 || run_as_invoke_user brew install openjdk@25
      if [[ -d /Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home ]]; then
        warn "可将 JAVA_HOME 设为：/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home"
      fi
      ;;
    linux)
      require_root_linux
      case "${OS_ID}" in
        ubuntu|debian)
          apt-get update -y
          apt-get install -y wget apt-transport-https gnupg
          mkdir -p /etc/apt/keyrings
          if [[ ! -f /etc/apt/keyrings/adoptium.asc ]]; then
            wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public \
              | tee /etc/apt/keyrings/adoptium.asc >/dev/null
          fi
          CODENAME="$(. /etc/os-release && echo "${VERSION_CODENAME}")"
          ARCH="$(dpkg --print-architecture)"
          echo "deb [arch=${ARCH} signed-by=/etc/apt/keyrings/adoptium.asc] https://packages.adoptium.net/artifactory/deb ${CODENAME} main" \
            > /etc/apt/sources.list.d/adoptium.list
          apt-get update -y
          apt-get install -y temurin-25-jdk
          ;;
        centos|rhel|rocky|almalinux|ol|fedora)
          cat >/etc/yum.repos.d/adoptium.repo <<'EOF'
[Adoptium]
name=Adoptium
baseurl=https://packages.adoptium.net/artifactory/rpm/centos/$releasever/$basearch
enabled=1
gpgcheck=1
gpgkey=https://packages.adoptium.net/artifactory/api/gpg/key/public
EOF
          if have_cmd dnf; then
            dnf -y install temurin-25-jdk
          else
            yum -y install temurin-25-jdk
          fi
          ;;
        *)
          err "暂不支持自动安装 JDK 的发行版：${OS_ID}，请手动安装 Temurin 25"
          exit 1
          ;;
      esac
      ;;
  esac
}

# ---- Maven ----
install_maven() {
  log "正在安装 Maven ..."
  case "${OS_FAMILY}" in
    macos)
      ensure_brew
      run_as_invoke_user brew install maven
      ;;
    linux)
      require_root_linux
      case "${OS_ID}" in
        ubuntu|debian)
          apt-get update -y
          apt-get install -y maven
          ;;
        centos|rhel|rocky|almalinux|ol|fedora)
          if have_cmd dnf; then
            dnf -y install maven
          else
            yum -y install maven
          fi
          ;;
        *)
          err "暂不支持自动安装 Maven 的发行版：${OS_ID}"
          exit 1
          ;;
      esac
      ;;
  esac
}

# ---- Node.js 22 ----
install_node() {
  log "正在安装 Node.js ..."
  case "${OS_FAMILY}" in
    macos)
      ensure_brew
      run_as_invoke_user brew install node@22 || run_as_invoke_user brew install node
      ;;
    linux)
      require_root_linux
      case "${OS_ID}" in
        ubuntu|debian)
          apt-get update -y
          apt-get install -y ca-certificates curl gnupg
          mkdir -p /etc/apt/keyrings
          if [[ ! -f /etc/apt/keyrings/nodesource.gpg ]]; then
            curl -fsSL https://deb.nodesource.com/gpgkey/nodesource-repo.gpg.key \
              | gpg --dearmor -o /etc/apt/keyrings/nodesource.gpg
          fi
          echo "deb [signed-by=/etc/apt/keyrings/nodesource.gpg] https://deb.nodesource.com/node_22.x nodistro main" \
            > /etc/apt/sources.list.d/nodesource.list
          apt-get update -y
          apt-get install -y nodejs
          ;;
        centos|rhel|rocky|almalinux|ol|fedora)
          curl -fsSL https://rpm.nodesource.com/setup_22.x | bash -
          if have_cmd dnf; then
            dnf -y install nodejs
          else
            yum -y install nodejs
          fi
          ;;
        *)
          err "暂不支持自动安装 Node 的发行版：${OS_ID}"
          exit 1
          ;;
      esac
      ;;
  esac

  # 前端包管理器 pnpm
  install_pnpm
}

# 安装 pnpm（若缺失）：优先 corepack，其次 npm 全局安装
install_pnpm() {
  if have_cmd pnpm; then
    return 0
  fi
  log "正在安装 pnpm ..."
  if have_cmd corepack; then
    corepack enable pnpm 2>/dev/null || true
  fi
  if ! have_cmd pnpm; then
    if [[ "${OS_FAMILY}" == "macos" ]]; then
      run_as_invoke_user npm i -g pnpm 2>/dev/null || npm i -g pnpm 2>/dev/null || true
    else
      npm i -g pnpm 2>/dev/null || true
    fi
  fi
  hash -r 2>/dev/null || true
  if have_cmd pnpm; then
    log "pnpm: $(pnpm -v)"
  else
    log "pnpm 安装未成功，请手动执行：corepack enable pnpm 或 npm i -g pnpm"
  fi
}

# ---- main ----
main() {
  detect_os
  echo
  log "======== 环境检查 ========"

  local need_docker=0 need_jdk=0 need_maven=0 need_node=0
  check_docker || need_docker=1
  check_jdk    || need_jdk=1
  check_maven  || need_maven=1
  check_node   || need_node=1

  if [[ "${need_docker}" -eq 0 && "${need_jdk}" -eq 0 && "${need_maven}" -eq 0 && "${need_node}" -eq 0 ]]; then
    echo
    log "环境齐全，可直接部署："
    log "  ./deploy-db.sh"
    log "  ./deploy-app.sh"
    exit 0
  fi

  if [[ "${CHECK_ONLY}" -eq 1 ]]; then
    echo
    err "存在缺失组件。请执行：./install-env.sh  （Linux 需 sudo）"
    exit 1
  fi

  echo
  log "======== 开始安装缺失组件 ========"
  # Linux 只要有任一系统包安装就要求 root
  if [[ "${OS_FAMILY}" == "linux" ]]; then
    require_root_linux
  fi

  [[ "${need_docker}" -eq 1 ]] && install_docker
  [[ "${need_jdk}" -eq 1 ]]    && install_jdk
  [[ "${need_maven}" -eq 1 ]]  && install_maven
  [[ "${need_node}" -eq 1 ]]   && install_node

  # 刷新当前 shell 可能看不到新 PATH，再检一次
  hash -r 2>/dev/null || true
  echo
  log "======== 安装后复查 ========"
  local failed=0
  check_docker || failed=1
  check_jdk    || failed=1
  check_maven  || failed=1
  check_node   || failed=1

  echo
  if [[ "${failed}" -ne 0 ]]; then
    warn "部分组件仍不可用（如 macOS 需先启动 Docker Desktop，或重新打开终端加载 PATH）"
    warn "请处理后再执行：./install-env.sh --check"
    exit 1
  fi

  log "环境就绪。下一步："
  log "  1. cp .env.example .env && vim .env"
  log "  2. ./deploy-db.sh"
  log "  3. ./deploy-app.sh"
}

main "$@"
