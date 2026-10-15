# 博客前台运行镜像：Nuxt SSR（Nitro `node-server` 产物）
#
# 和 server / web 一样，镜像里只放**本机已打好的产物**，不在 Docker 内装依赖。
# 构建前请先在仓库根目录执行：
#   cd designer-ui/blog-web && pnpm install --frozen-lockfile && pnpm run build
# 再由 deploy-app.sh 把 .output 同步到 build/blog-web/
# 构建上下文：deploy/

FROM node:22-alpine
LABEL maintainer="designer-admin" \
      app="designer-blog-web"

# Nitro 的 .output 是自包含的（运行时依赖已打进 .output/server/node_modules），
# 所以运行阶段既不需要 package.json，也不需要 npm install。
WORKDIR /app

COPY build/blog-web/.output /app

ENV NODE_ENV=production \
    NITRO_HOST=0.0.0.0 \
    NITRO_PORT=3000 \
    TZ=Asia/Shanghai

# 用非 root 运行：node 镜像自带 uid=1000 的 node 用户，
# 产物是只读的（默认 644），Nitro 的缓存走内存驱动，不需要写权限。
USER node
EXPOSE 3000

# 只验证 Nuxt 进程自身。/health 由 server/routes/health.get.js 提供，刻意不碰后端：
# 后端抖动不该把前台容器判定为不健康。
HEALTHCHECK --interval=15s --timeout=5s --start-period=20s --retries=5 \
  CMD wget -qO- http://127.0.0.1:3000/health >/dev/null || exit 1

CMD ["node", "/app/server/index.mjs"]
