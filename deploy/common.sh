#!/usr/bin/env bash
# 公共函数（被 deploy-*.sh / stop.sh 等 source）
# shellcheck shell=bash

ensure_env_file() {
  if [[ ! -f .env ]]; then
    echo -e "\033[1;33m[WARN]\033[0m .env 不存在，从 .env.example 复制"
    cp .env.example .env
    echo -e "\033[1;33m[WARN]\033[0m 已生成默认 .env，生产环境请务必修改密码与连接信息"
  fi
}

env_get() {
  local key="$1" line val
  line="$(grep -E "^${key}=" .env | tail -n1 || true)"
  [[ -z "${line}" ]] && { printf ''; return; }
  val="${line#*=}"
  if [[ "${val}" =~ ^\".*\"$ ]]; then
    val="${val:1:${#val}-2}"
  elif [[ "${val}" =~ ^\'.*\'$ ]]; then
    val="${val:1:${#val}-2}"
  fi
  printf '%s' "${val}"
}

need_docker() {
  if ! command -v docker >/dev/null 2>&1; then
    echo -e "\033[0;31m[ERROR]\033[0m 未找到 docker，请先执行：./install-env.sh（Linux 需 sudo）" >&2
    exit 1
  fi
  if ! docker compose version >/dev/null 2>&1; then
    echo -e "\033[0;31m[ERROR]\033[0m 未找到 docker compose 插件，请先执行：./install-env.sh（Linux 需 sudo）" >&2
    exit 1
  fi
}

# 项目名与 compose 文件
compose_db() {
  local project
  project="$(env_get COMPOSE_PROJECT_NAME)"
  project="${project:-designer-admin}-db"
  docker compose --env-file .env -p "${project}" -f docker-compose.db.yml "$@"
}

compose_app() {
  local project
  project="$(env_get COMPOSE_PROJECT_NAME)"
  project="${project:-designer-admin}-app"
  docker compose --env-file .env -p "${project}" -f docker-compose.app.yml "$@"
}

compose_minio() {
  local project
  project="$(env_get COMPOSE_PROJECT_NAME)"
  project="${project:-designer-admin}-minio"
  docker compose --env-file .env -p "${project}" -f docker-compose.minio.yml "$@"
}
