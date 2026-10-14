# 前端运行镜像：同时托管博客前台（blog-web）与管理后台（admin-web）
# 构建前请先打好两个前端静态资源：
#   cd designer-ui/admin-web && pnpm install --frozen-lockfile && pnpm run build
#   cd designer-ui/blog-web  && pnpm install --frozen-lockfile && pnpm run build
# 构建上下文：deploy/

FROM nginx:1.27-alpine
LABEL maintainer="designer-admin" \
      app="designer-web"

# 单容器 + 两个 server 块，按 server_name（www.rainterm.cn / admin.rainterm.cn）分流
COPY nginx/default.conf /etc/nginx/conf.d/default.conf
# 两个站点各自独立 root
COPY build/blog-web/dist /usr/share/nginx/html/blog
COPY build/web/dist /usr/share/nginx/html/admin

EXPOSE 80

# 不带 Host 请求会落到 default_server（博客），其 /health 为容器自检，不依赖后端
HEALTHCHECK --interval=15s --timeout=5s --start-period=10s --retries=3 \
  CMD wget -qO- http://127.0.0.1/health >/dev/null || exit 1
