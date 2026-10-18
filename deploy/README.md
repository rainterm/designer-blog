# Designer Blog — 生产环境 Docker 部署

数据库与应用**分开**部署；应用在**本机用 JDK / Maven / Node 打包**后，再打成运行镜像。
应用共三个容器：后端 Server、博客前台（Nuxt SSR 的 Node 服务）、Nginx 入口。

## 目录

```
deploy/
├── install-env.sh          # 检查并安装 Docker / JDK / Maven / Node
├── deploy-db.sh            # 仅部署 MySQL + Redis
├── deploy-minio.sh         # 本地 MinIO（S3 兼容对象存储，可选）
├── deploy-app.sh           # 本地打包 + 构建镜像 + 启动应用
├── stop.sh / restart.sh / logs.sh
├── .env.example
├── docker-compose.db.yml
├── docker-compose.app.yml
├── build/                  # 本地打包产物暂存（自动生成）
│   ├── server/             #   后端胖包
│   ├── web/dist            #   admin-web 静态资源
│   └── blog-web/.output    #   blog-web 的 Nuxt SSR 产物（含自包含的 node_modules）
├── server/Dockerfile         # 后端 Server（JRE 运行镜像）
├── blog/Dockerfile           # 博客前台（Node 运行 Nuxt SSR 产物）
├── web/Dockerfile            # 入口 Nginx（托管 admin-web 静态资源 + 反代博客、后端）
└── nginx/default.conf        # 按 server_name 分流（博客 / 后台）并反代 /app-api、/admin-api
```

### 为什么博客前台是独立容器

博客前台已从「Vue SPA + 静态托管」改造为 **Nuxt SSR**，而 SSR 必须按请求实时渲染 HTML
（爬虫与微信/Twitter 等社交分享爬虫**不执行 JS**，它们拿到的只能是服务端已经渲染好的页面）。
nginx 只能发文件，做不到这件事，所以 `blog-web` 的产物是一个 Node 服务（`blog` 容器）。

管理后台仍是纯静态，继续由入口 nginx 直接托管。三个容器的分工：

| 容器 | 角色 | 对外 |
|---|---|---|
| `server` | 后端 API（`:8080`） | 只绑宿主回环 |
| `blog` | 博客前台 Nuxt SSR（`:3000`） | 不发布端口，仅经 nginx |
| `web` | 入口 nginx（`:80`），按域名分流 | 宿主 `WEB_PORT` |

按域名（`server_name`）分流：

| 域名 | 站点 | 说明 |
|---|---|---|
| `www.rainterm.cn` | 博客前台 blog | 对外主站，也是 `default_server`（直接访问 IP 会落到这里）；**反代到 `blog:3000`** |
| `admin.rainterm.cn` | 管理后台 admin-web | 需登录，账号 `admin / admin123`；本容器静态资源 |
| `rainterm.cn` | — | 301 跳到 `www.rainterm.cn` |
| 其它 Host | — | 落到博客前台（不暴露后台） |

后台站点各有一份独立 `root`（`/usr/share/nginx/html/admin`）；
博客前台**不暴露 `/admin-api`**（显式返回 404），前台即使被扫描也进不了后台 API。

> **SSR 取数与客户端取数是两条路，这是有意设计的。**
> 服务端渲染期间，Node 直连容器内网 `http://server:8080`（`NUXT_API_INTERNAL_BASE`），
> 于是首屏正文、评论、站点设置全部随 HTML 一起返回；
> 而浏览器侧的请求（评论提交 / 访问埋点）仍走 `/app-api/` 经 nginx 反代。
> 之所以要分开：直连内网时后端看到的 IP 是 `blog` 容器，**不是访客**，
> 所以任何依赖真实 IP 的能力（限流、IP 归属地、访问统计）都必须留在浏览器侧发起。
>
> **滑块验证码的路径：插件原生 + 自己包的公开前缀。**
> 后端插件把端点注册在根路径 `/captcha/get`、`/captcha/check`（路径不可配置），
> framework 里的 `AppCaptchaController` 另外映射 `/app-api/captcha/**`，
> 复用同一个 `CaptchaService` 并用插件的 `getRemoteId` 保证 `browserInfo` 一致。
> 因此浏览器侧只有一套 URL：`/app-api/captcha/get`、`/app-api/captcha/check`，
> nginx 与 dev 代理都按普通 `/app-api` 转发，**不做前缀改写**；
> 两个站点的 nginx 仍各有 `location /app-api/captcha/`，只为用独立的限流桶
> （`blog_captcha` / `admin_captcha`）。插件原生的 `/captcha/**` 两个站点都显式 404。
> 漏掉任何一条，请求会落到该站点的兜底 location（`location /` 或 `try_files`）上，
> 拿到一个 HTML 而不是 JSON —— 症状是「验证码加载失败」而不是 404，排查时容易走弯路。

> 路由用的域名只在 `nginx/default.conf` 里定义（部署脚本不依赖它，只在结束提示里打印这两个地址）。
> 改域名只需改该文件的 `server_name`，然后 `./deploy-app.sh --skip-build --rebuild`。
> `sitemap.xml` / `robots.txt` 里的绝对地址由请求的 `Host` 推导，无需跟着改。

## 推荐流程

```bash
cd designer-blog/deploy

# 1. 检查环境，缺什么装什么（Linux 建议 sudo）
./install-env.sh
# 仅检查不安装：
./install-env.sh --check

# 2. 配置
cp .env.example .env && vim .env

# 3. 启动数据库
./deploy-db.sh

# 4. 初始化数据库表（首次 / 重置时执行；破坏性操作，请勿对有数据的库重复执行）
(cd .. && bash sql/init/mysql/00-run-init.sh)

# 5. 打包并部署应用
./deploy-app.sh
```

`./install-env.sh` 会检查并按需安装：

| 组件 | 要求 |
|---|---|
| Docker + Compose | 运行容器 |
| JDK 25 (Temurin) | 后端编译 |
| Maven | 后端打包 |
| Node.js 18+（默认装 22） | 前端构建 |

支持 macOS（Homebrew）与 Ubuntu/Debian/RHEL 系。

| 入口 | 默认端口 | 说明 |
|---|---|---|
| 博客前台 | `http://www.rainterm.cn` | blog，Nuxt SSR 对外主站（正文随首屏 HTML 返回） |
| 收录入口 | `http://www.rainterm.cn/sitemap.xml` | 全站 URL 清单，提交给百度/Google |
| 管理后台 | `http://admin.rainterm.cn` | admin-web，账号 `admin / admin123` |
| 后端直连 | `SERVER_PORT=8080` | 只绑回环，一般不必对外 |

`WEB_PORT` 默认为 `80`。若改成其它端口（如 `8080` 被占时），访问地址带上端口即可（`http://www.rainterm.cn:8080`）。

### DNS 解析

两个域名都指向同一台服务器，统一走 80：

```
www.rainterm.cn     A   <服务器IP>     # → 博客前台
admin.rainterm.cn   A   <服务器IP>     # → 管理后台
rainterm.cn         A   <服务器IP>     # → 301 跳到 www
```

解析生效后即可用域名访问，**无需改 nginx 配置**（域名已在 `nginx/default.conf` 里配好）。
裸域 `rainterm.cn` 那一条只用于跳转，没有该解析也不影响前两条。

## 使用外部数据库

配置 `.env` 中 `DB_*` / `REDIS_*` 后只执行 `./deploy-app.sh`。建表请执行 `sql/init/mysql/00-run-init.sh`（按 `_base` → `system` / `blog` / `demo` 分目录导入）；脚本支持交互输入，也可用 `DB_HOST/DB_PORT/DB_USER/DB_PASSWORD/DB_NAME` 环境变量免交互。

## 常用命令

| 命令 | 说明 |
|---|---|
| `./install-env.sh` | 检查并安装环境 |
| `./deploy-db.sh` | 启动 MySQL、Redis（不自动建表） |
| `./deploy-minio.sh` | 启动本地 MinIO（S3 API `9000` / Console `9001`，可选） |
| `./deploy-app.sh` | 本地打包并部署应用（Server + 博客前台 Node + 入口 Nginx） |
| `./deploy-app.sh --skip-build` | 跳过打包，用已有 `deploy/build` |
| `./restart.sh app` / `./stop.sh app` | 重启 / 停止应用（三个容器一起） |
| `./logs.sh server` | 后端日志（容器 stdout，受 compose `logging` 限制） |
| `./logs.sh blog` | 博客前台（Nuxt SSR）日志：SSR 报错、渲染异常看这里 |
| `./logs.sh web` | 入口 Nginx 日志（两个站点共用，日志内以 `$host` 区分） |

## 日志

- **文件日志**：容器内 `/app/logs/designer-server.log`，挂载在 `server_logs` 卷（`docker volume inspect` 查看位置）。按「天 + 单文件 10MB」滚动，默认保留 `maxHistory=7` 天 / 总量上限 `totalSizeCap=1GB`。
- **容器 stdout**：`./logs.sh server` 看到的是 Docker 收集的标准输出（应用控制台日志 + 启动报错），由 `docker-compose.app.yml` 的 `logging.options.max-size=50m` / `max-file=5` 上限约束，最多约 250MB。
- **调整保留策略**：改后端配置 `solon.logging.appender.file.maxFileSize` / `maxHistory` / `totalSizeCap`，或用环境变量 `LOG_PATH` 覆盖文件路径（默认 `logs/designer-server`）。

## 架构

```
./install-env.sh  →  Docker + JDK + Maven + Node
本机打包：
  mvn -pl designer-server -am package      # → designer-server/target/designer-server.jar
  designer-ui/admin-web → pnpm run build   # → dist/（静态）
  designer-ui/blog-web  → pnpm run build   # → .output/（Nuxt SSR，Node 直接运行）
产物同步到 deploy/build/{server,web,blog-web}
Docker 运行镜像（JRE / Node / Nginx）→ MySQL / Redis
```

## 注意事项

1. 修改 `.env` 示例密码；本机库时 `DB_PASSWORD` 与 `MYSQL_ROOT_PASSWORD` 一致。
2. macOS 安装 Docker Desktop 后需手动打开一次。
3. 日常发版只跑 `./deploy-app.sh`，不必动数据库。
4. 数据库表需手动初始化（`bash sql/init/mysql/00-run-init.sh`）；该脚本为**破坏性**（`DROP TABLE` 重建 + 插入种子数据），仅在首次或需要重置时执行，勿对有数据的库重复运行。
5. 后台域名建议加访问控制：`admin.rainterm.cn` 虽需登录，但仍暴露在公网。可在防火墙/安全组限制来源 IP，或再加一层 Basic Auth。
6. 当前为 HTTP（80）。后续上 HTTPS 时，在 `nginx/default.conf` 里增加 443 server 块并把 80 跳转到 443 即可；两个站点共用一张证书（`www.rainterm.cn` + `admin.rainterm.cn`）。
7. 生产环境建议只放行 80/443 与 SSH；`SERVER_PORT`（8080）只绑回环，无需对外开放。
8. 若 `WEB_PORT` 用不上 80（例如被其它服务占用），改 `.env` 即可，两个站点的域名分流不受影响。
9. **SSR 页面缓存在 nginx 层**（`nginx/default.conf` 的 `proxy_cache_path` + `location /`）：
   缓存整段渲染好的 HTML，TTL **60 秒**，命中时完全不经过 Node。
   能开的前提是 SSR 输出对所有访客一致 —— 评论只取已审核内容，评论者身份 / 验证码 / 埋点
   都只在浏览器侧执行；阅读数也挂在那些埋点上，缓存命中时照常上报，所以不会失真。
   几处刻意的设定：缓存键含 `$host`（否则会把别的域名的 HTML 发出去）、`max_size=100m`
   （否则随机 query 能把缓存撑爆）、**404 也缓存**（随机 slug 扫描到不了 Node）、
   `proxy_cache_lock`（冷 URL 上的并发只回源一次）、`proxy_cache_use_stale`
   （Node 挂掉时给旧页面而不是 502）；`/app-api/` 与 `/health` 显式 `proxy_cache off`。
   排查用响应头 `X-Cache-Status`（`HIT` / `MISS` / `STALE`）；代价是评论与站点设置最多滞后 60 秒。
10. 博客前台是 Node 常驻进程，**内存占用与静态托管不在一个量级**（基线约 100–200MB）。
    服务器内存紧张时，优先调低 `JAVA_OPTS` 的 `-Xmx`（后端是大头），而不是动 Node。
11. **限流在 nginx 层**（`nginx/default.conf` 顶部的 `limit_req_zone` / `limit_conn_zone`）：
    读路径按 IP 60r/m（`burst=30 nodelay`）、`/app-api/` 120r/m、`/sitemap.xml` 6r/m，超限回 **429**。
    `/_nuxt/` 静态资源与两个 `/health` 刻意不限流（前者一次加载十几个 chunk，后者是 15s 一次的容器自检）。
    这几个桶都在 nginx 里，**不消耗 Node 与数据库**；评论与访问埋点在应用层还有一层更细的 Redis 限流。
    调整阈值只改 `rate=`；`limit_conn` 是单 IP 并发连接上限（20）。
    ⚠ 若将来在 nginx 前再加 CDN，必须用 `real_ip` / `set_real_ip_from` 还原 `$remote_addr`，
    否则限流会退化成「按 CDN 节点 IP 统计」，一个节点超限会误伤该节点下所有访客 ——
    与「限流误伤」那条是同一类坑。
