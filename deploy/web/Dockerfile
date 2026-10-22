# 前端运行镜像：使用本地 Node/Vite 打好的静态资源
# 构建前请先：cd designer-ui/admin-web && pnpm install --frozen-lockfile && pnpm run build
# 构建上下文：deploy/

FROM nginx:1.27-alpine
LABEL maintainer="designer-admin" \
      app="designer-admin-web"

COPY nginx/default.conf /etc/nginx/conf.d/default.conf
COPY build/web/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=15s --timeout=5s --start-period=10s --retries=3 \
  CMD wget -qO- http://127.0.0.1/ >/dev/null || exit 1
