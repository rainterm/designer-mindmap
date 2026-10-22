# 后端运行镜像：使用本地 Maven 打好的胖包
# 运行镜像：仅拷贝已在本机打好的胖包
# 构建前请先在仓库根目录：
#   mvn -pl designer-server -am -DskipTests clean package
# 再由 deploy-app.sh 将 jar 拷到 build/server/
# 构建上下文：deploy/

FROM eclipse-temurin:25-jre
LABEL maintainer="designer-admin" \
      app="designer-admin-server"

WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir -p /app/logs /app/uploads /app/data/agent-harness \
    && groupadd -r app && useradd -r -g app -d /app -s /sbin/nologin app \
    && chown -R app:app /app

COPY build/server/designer-server.jar /app/app.jar

ENV JAVA_OPTS="-Xms512m -Xmx1024m -Dfile.encoding=UTF-8" \
    DB_HOST=host.docker.internal \
    DB_PORT=3306 \
    DB_NAME=designer_agent \
    DB_USER=root \
    DB_PASSWORD=root \
    REDIS_HOST=host.docker.internal \
    REDIS_PORT=6379 \
    REDIS_PASSWORD= \
    REDIS_DB=5 \
    TZ=Asia/Shanghai

USER app
EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=40s --retries=5 \
  CMD curl -fsS http://127.0.0.1:8080/admin-api/system/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
