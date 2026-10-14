# Designer Blog — 生产环境 Docker 部署

数据库与应用**分开**部署；应用在**本机用 JDK / Maven / Node 打包**后，再打成运行镜像。
前端由同一个 Nginx 容器按域名分流托管博客前台与管理后台。

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
│   └── blog-web/dist       #   blog-web 静态资源
├── server/Dockerfile         # 后端 Server（JRE 运行镜像）
├── web/Dockerfile            # 前端 Nginx 运行镜像（同容器托管 admin-web + blog-web）
└── nginx/default.conf        # 按 server_name 分流（博客 / 后台）并反代 /app-api、/admin-api
```

两个前端站点由**同一个 nginx 容器**托管，按域名（`server_name`）分流：

| 域名 | 站点 | 说明 |
|---|---|---|
| `www.rainterm.cn` | 博客前台 blog-web | 对外主站，也是 `default_server`（直接访问 IP 会落到这里） |
| `admin.rainterm.cn` | 管理后台 admin-web | 需登录，账号 `admin / admin123` |
| `rainterm.cn` | — | 301 跳到 `www.rainterm.cn` |
| 其它 Host | — | 落到博客前台（不暴露后台） |

之所以合成一个容器：两个站点要**共用宿主 80 端口**并按域名区分，只能有一个进程监听 80。
两个站点仍各有独立 `root`（`/usr/share/nginx/html/blog` 与 `/html/admin`），互不可达；
且博客站点**不暴露 `/admin-api`**（显式返回 404），前台即使被扫描也进不了后台 API。

> 路由用的域名只在 `nginx/default.conf` 里定义（部署脚本不依赖它，只在结束提示里打印这两个地址）。
> 改域名只需改该文件的 `server_name`（顺带更新 `deploy-app.sh` 结尾的两行提示），然后
> `./deploy-app.sh --skip-build --rebuild`。

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
| 博客前台 | `http://www.rainterm.cn` | blog-web，对外主站 |
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
| `./deploy-app.sh` | 本地打包并部署应用（Server + 前端 Nginx） |
| `./deploy-app.sh --skip-build` | 跳过打包，用已有 `deploy/build` |
| `./restart.sh app` / `./stop.sh app` | 重启 / 停止应用（含博客前台与管理后台） |
| `./logs.sh server` | 后端日志（容器 stdout，受 compose `logging` 限制） |
| `./logs.sh web` | 前端 Nginx 日志（两个站点共用，日志内以 `$host` 区分） |

## 日志

- **文件日志**：容器内 `/app/logs/designer-server.log`，挂载在 `server_logs` 卷（`docker volume inspect` 查看位置）。按「天 + 单文件 10MB」滚动，默认保留 `maxHistory=7` 天 / 总量上限 `totalSizeCap=1GB`。
- **容器 stdout**：`./logs.sh server` 看到的是 Docker 收集的标准输出（应用控制台日志 + 启动报错），由 `docker-compose.app.yml` 的 `logging.options.max-size=50m` / `max-file=5` 上限约束，最多约 250MB。
- **调整保留策略**：改后端配置 `solon.logging.appender.file.maxFileSize` / `maxHistory` / `totalSizeCap`，或用环境变量 `LOG_PATH` 覆盖文件路径（默认 `logs/designer-server`）。

## 架构

```
./install-env.sh  →  Docker + JDK + Maven + Node
本机打包：
  mvn -pl designer-server -am package
  designer-ui/admin-web → pnpm run build
  designer-ui/blog-web  → pnpm run build
产物同步到 deploy/build/{server,web,blog-web}
Docker 运行镜像（JRE / Nginx）→ MySQL / Redis
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
