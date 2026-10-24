#!/usr/bin/env bash
# =====================================================================
#  MySQL 初始化入口（Docker entrypoint 与本机均可执行）
#  顺序：_base → system → demo …
#
#  连接信息：环境变量优先（DB_HOST / DB_PORT / DB_USER / DB_PASSWORD / DB_NAME，
#    兼容 MYSQL_HOST / MYSQL_PORT / MYSQL_ROOT_USER / MYSQL_ROOT_PASSWORD / MYSQL_PWD / MYSQL_DATABASE）；
#    交互终端下仅对「未提供」的项提示输入，全部已提供则不提示；非交互环境不提示。
#  数据库名默认 designer_mindmap（与 designer-server/src/main/resources/app.yml 的 DB_NAME 默认值一致），可自定义。
#
#  新增业务模块：建同名目录（含 schema.sql / data.sql），并加入下方 MODULES
# =====================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# SQL 文件里写死的库名占位符（统一为 app.yml 的默认库名）
# 执行时会被替换成实际的 DB_NAME，因此 SQL 文件本身无须感知自定义库名
SQL_DB_PLACEHOLDER="designer_mindmap"

# 业务模块目录名（按依赖顺序；不含 _base）
MODULES=(system demo agent mindmap)

# ---------------------------------------------------------------------
# 收集连接信息
# ---------------------------------------------------------------------
DB_HOST="${DB_HOST:-${MYSQL_HOST:-}}"
DB_PORT="${DB_PORT:-${MYSQL_PORT:-}}"
DB_USER="${DB_USER:-${MYSQL_ROOT_USER:-}}"
DB_PASSWORD="${DB_PASSWORD:-${MYSQL_ROOT_PASSWORD:-${MYSQL_PWD:-}}}"
DB_NAME="${DB_NAME:-${MYSQL_DATABASE:-}}"

# 读取一个输入项：$1=提示 $2=默认值 → 打印结果（回车沿用默认值）
ask() {
  local tip="$1" def="${2:-}" val=""
  if [[ -n "${def}" ]]; then
    read -r -p "${tip} [${def}]: " val || true
  else
    read -r -p "${tip}: " val || true
  fi
  printf '%s' "${val:-${def}}"
}

# 读取密码：不回显
ask_secret() {
  local tip="$1" val=""
  read -r -s -p "${tip}: " val || true
  echo >&2
  printf '%s' "${val}"
}

# 交互终端：只补全「环境变量未提供」的项；全部已提供则不提示
# 非交互（Docker / CI）：直接用环境变量与默认值，不提示
if [[ -t 0 && -t 1 ]]; then
  if [[ -z "${DB_HOST}" || -z "${DB_PORT}" || -z "${DB_USER}" || -z "${DB_PASSWORD}" || -z "${DB_NAME}" ]]; then
    echo "[sql-init] 补全数据库连接信息（回车使用默认值；已由环境变量提供的项会跳过）"
  fi
  [[ -n "${DB_HOST}" ]] || DB_HOST="$(ask 'MySQL 主机（留空走本地 socket）')"
  [[ -n "${DB_PORT}" ]] || DB_PORT="$(ask 'MySQL 端口' '3306')"
  [[ -n "${DB_USER}" ]] || DB_USER="$(ask 'MySQL 用户' 'root')"
  [[ -n "${DB_PASSWORD}" ]] || DB_PASSWORD="$(ask_secret 'MySQL 密码（无密码直接回车）')"
  [[ -n "${DB_NAME}" ]] || DB_NAME="$(ask '数据库名' "${SQL_DB_PLACEHOLDER}")"
else
  DB_PORT="${DB_PORT:-3306}"
  DB_USER="${DB_USER:-root}"
  DB_NAME="${DB_NAME:-${SQL_DB_PLACEHOLDER}}"
fi

# 数据库名会作为标识符代入 SQL，限定为字母/数字/下划线
if [[ ! "${DB_NAME}" =~ ^[A-Za-z0-9_]+$ ]]; then
  echo "[sql-init] 非法数据库名：${DB_NAME}（仅允许字母、数字、下划线）" >&2
  exit 1
fi

# ---------------------------------------------------------------------
# 组装 mysql 客户端命令
# ---------------------------------------------------------------------
mysql_cmd=(mysql -u "${DB_USER}" --default-character-set=utf8mb4)
# 未指定主机时走本地 socket（Docker entrypoint / 本机 socket 均适用）
if [[ -n "${DB_HOST}" ]]; then
  mysql_cmd+=(-h "${DB_HOST}" -P "${DB_PORT}")
fi
if [[ -n "${DB_PASSWORD}" ]]; then
  mysql_cmd+=(-p"${DB_PASSWORD}")
fi

echo "[sql-init] host=${DB_HOST:-<local socket>} port=${DB_PORT} user=${DB_USER} db=${DB_NAME}"

run_file() {
  local file="$1"
  [[ -f "${file}" ]] || return 0
  echo "[sql-init] $(basename "$(dirname "${file}")")/$(basename "${file}")"
  # 把 SQL 里写死的库名占位符替换为实际库名，支持自定义 DB_NAME
  sed "s/\`${SQL_DB_PLACEHOLDER}\`/\`${DB_NAME}\`/g" "${file}" | "${mysql_cmd[@]}"
}

run_dir_ordered() {
  local dir="$1"
  [[ -d "${dir}" ]] || return 0
  # 固定顺序：schema → data → 其余按文件名
  local f
  for f in schema.sql data.sql; do
    run_file "${dir}/${f}"
  done
  local extra
  shopt -s nullglob
  for extra in "${dir}"/*.sql; do
    case "$(basename "${extra}")" in
      schema.sql|data.sql) continue ;;
      *) run_file "${extra}" ;;
    esac
  done
  shopt -u nullglob
}

echo "[sql-init] root=${ROOT}"

run_dir_ordered "${ROOT}/_base"

for module in "${MODULES[@]}"; do
  run_dir_ordered "${ROOT}/${module}"
done

# 老库增量脚本放最后：全新库上它们应是幂等空转，老库则补齐新增菜单 / 字段
run_dir_ordered "${ROOT}/../../upgrade/mysql"

echo "[sql-init] done"
