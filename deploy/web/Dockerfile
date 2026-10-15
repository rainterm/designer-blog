# 前端运行镜像：托管管理后台（admin-web）静态资源 + 站点分流入口
#
# 博客前台（blog-web）自 Nuxt SSR 改造后**不再由这个容器托管**：
# 它需要按请求实时渲染 HTML，是一个独立的 Node 服务（见 deploy/blog/Dockerfile）。
# 本容器只保留两个职责：
#   1. 托管 admin-web 静态资源；
#   2. 按域名分流 —— 博客域名反代到 blog 容器，后台域名留给静态文件。
#
# 构建前请先打好后台静态资源：
#   cd designer-ui/admin-web && pnpm install --frozen-lockfile && pnpm run build
# 构建上下文：deploy/

FROM nginx:1.27-alpine
LABEL maintainer="designer-admin" \
      app="designer-web"

# 单容器 + 三个 server 块，按 server_name（www.rainterm.cn / admin.rainterm.cn / 裸域）分流
COPY nginx/default.conf /etc/nginx/conf.d/default.conf
# 后台静态资源（博客前台不在这个镜像里）
COPY build/web/dist /usr/share/nginx/html/admin

# SSR 页面缓存目录（proxy_cache_path 见 nginx/default.conf）。
# 官方镜像里 /var/cache/nginx 属主是 root，而 worker 以 nginx 运行，不授权会在
# 首次回源时报「failed to create cache file (13: Permission denied)」。
RUN mkdir -p /var/cache/nginx/blog \
    && chown -R nginx:nginx /var/cache/nginx

EXPOSE 80

# 容器自检打 /nginx-health：它只验证 nginx 自己在本容器内可用。
# 不打 /health（那会反代到 blog 容器），否则前台一抖动这个容器也会被标成 unhealthy。
HEALTHCHECK --interval=15s --timeout=5s --start-period=10s --retries=3 \
  CMD wget -qO- http://127.0.0.1/nginx-health >/dev/null || exit 1
