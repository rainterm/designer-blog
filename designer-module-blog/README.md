# designer-module-blog

博客业务模块。

## 结构

```
designer-module-blog/
├── blog-api/       # 按需 *Api；Api 自用入参/出参类型（勿放业务 model）
└── blog-service/   # ApiImpl、Service、Controller、Mapper、model
```

`designer-server` 依赖 `blog-service`。跨模块调用时只依赖 `blog-api`，并按需新增 `...api.XxxApi`。

初始化 SQL：`sql/init/mysql/blog/`。

## 后台评论管理

后台评论接口（`/admin-api/blog/comment/**`）需 `blog:comment:*` 权限，由 `SaCheckPermission` 校验；
超级管理员返回通配符 `*` 直接放行（见 `StpInterfaceImpl`）。

| 接口 | 权限 | 说明 |
|------|------|------|
| `GET /page` | `blog:comment:list` | 分页查询（返回实体，含 `ip` / `ipLocation`） |
| `PUT /audit` | `blog:comment:audit` | 审核：1 通过 / 2 拒绝 |
| `DELETE /remove` | `blog:comment:remove` | 批量删除 |
| `POST /reply` | `blog:comment:reply` | **管理员回复**，见下 |

`POST /reply` 入参 `{ id, content }`，把回复挂到 `id` 指向的评论下：

- 不走验证码与频率限制——这是站长的正常运营动作；
- `status` 直接置 1，回复即刻公开；
- **不记录 IP / 归属地**：回复代表站点方，公开归属地会暴露站长位置，没有展示价值；
- **署名不接受入参**，固定取「站点设置 → 站长署名」：回复弹窗里若再给一个昵称输入框，
  就会出现同一个人顶着多个署名的情况，与 `author_type=1` 这个统一身份标记相矛盾。
  要改署名去站点设置改，所有回复一起生效（弹窗里只做只读展示）；
- 不限回复层级：显示层会统一压平为 2 层（见下），`parent_id` 只用于记录「回复了谁」；
- **只能回复已通过（`status=1`）的评论**，否则报「只能回复已通过的评论，请先审核通过」。

最后一条的原因：回复会以 `status=1` 立即公开，而公开列表会过滤掉未通过的父评论，
并把孤儿子评论**提升为顶级显示**。若允许回复待审评论，读者就会看到一条没有上下文的博主发言。
先审核再回复，公开列表里的对话才是完整的。
前端相应地只对已通过的行渲染「回复」按钮（待审行走「通过 / 拒绝」流程）。

前端在 `designer-ui/admin-web/src/modules/blog/views/comment/index.vue`：
操作列「回复」按钮打开弹窗，弹窗内引用被回复的原文，避免在平铺列表里回错对象；
列表里 `parentId != 0` 的行会带一个「回复」标签以便区分。

## 公开评论的防刷

`/app-api/blog/public/comments` 是匿名接口（`@SaIgnore`），登录态和权限注解都用不上，
因此由**滑块验证码**（`CaptchaVerifier`）+ 控制层注解（频率限制）做准入控制：

- **滑块验证码**：走 AJ-Captcha（`captcha-solon-plugin`）的两段式流程，
  实现与配置都在 `designer-framework` 的 `com.designer.admin.framework.captcha`
  —— **与后台登录共用同一套端点与存储**，不是评论专属：
  - `POST /captcha/get` 下发底图 + 拼图 + token；
  - 前端拖到对齐后 `POST /captcha/check`，服务端比对偏移量，通过则下发
    `captchaVerification`（AES 加密的一次性凭据）；
  - 提交评论时带 `captchaVerification`，服务端用 `CaptchaVerifier#assertVerified`
    复核并**一次性消费**（用到即失效，所以同一张验证码放行不了两次提交）。

  这两个端点由插件自带的 controller 注册在**根路径**下（不在 `/app-api` 前缀里），
  因此 nginx 的博客站点块里为它单独反代了一次（见 `deploy/nginx/default.conf`）。
  「要不要验」是业务策略，不在这两个端点里：评论只看站点总开关。
- **频率限制**（按客户端 IP）：阈值写在 `BlogPublicController` 的 `@RateLimit` 注解上，
  不需要手写计数代码，也不用为「让注解生效」在 service 里造中间方法
  （注解拦截只对**跨 bean 调用**生效，标在控制层最省心）：
  - 两次提交最小间隔 30 秒
  - 24 小时内最多 20 条

  验证码**拉取**的频控不在 `@RateLimit` 上 —— 注解标不到第三方 controller 上，
  改由 AJ-Captcha 自带的 `req-*`（按 `clientUid` 计数，见 `app.yml`）
  加 nginx 的 `blog_captcha` 桶（按 IP）承担。

  底层是 `RedisCounter` 的**固定窗口**（窗口锚在窗口内第一次请求，不会被后续请求续期，
  到期键自动消失），所以多实例部署时阈值不会被放大成「实例数 × 阈值」。
  ⚠️ 不要改回 `redisx` 的 `s.key(k).incr()` + `expire()` 写法 —— 那个 `expire`
  只是往会话对象上记了个数、并不下发命令，键会**永不过期**，
  一旦超阈值该 IP 就被永久拉黑（踩过一次，见 `RedisCounter` 的类注释）。
- **顺序**：限流在方法体之前执行，所以它挡在验证码校验**前面** ——
  连滑块都没拖的垃圾请求也会被限速，代价是**拖错滑块同样占用一次额度**。
  同一注解内的多条规则按声明顺序校验，"间隔" 写在 "配额" 前面，
  被间隔挡下的突发请求就不会白吃掉日配额。
  方法体内部的顺序是「站点总开关 → 验证码 → 落库」：站点关闭时立刻拒绝，
  免得白白消费掉访客的一张验证码（凭据一次性，消费了就得重拖）。
- **回复层级压平为 2 层**：存储上 `parent_id` 记录真实回复对象（链可以任意深），
  但生成评论树时一律上溯到所在**顶级评论**再挂到它下面，所以返回结构最多两层：
  顶级评论 + 其下全部回复。第 2 层的回复再被回复，仍然显示在第 2 层，
  由 `replyToNickname` 标出「某某 回复 某某」。
  这样前台缩进最多一级、不会出现越缩越窄的深链，同一串对话也平铺在一起。
  上溯用「迭代 + 记忆化」（`BlogCommentService#resolveRootId`）实现，
  既不受父链深度限制，也不会递归爆栈或退化成 O(n²)。
- **评论分页**：`GET /posts/{id}/comments?pageNum=&pageSize=` 按**顶级评论**分页
  （返回 `PageResult`），每条顶级评论带完整子树。树形结构没法直接交给 SQL 分页，
  所以先在内存里按顶级整理再切页；返回体大小随页大小收敛，也保留了「父评论未通过时
  子评论提升为顶级」的原有语义。
- 校验顺序见上：限流在最外层（方法调用之前），方法体内是「站点总开关 → 滑块验证码 → 落库」。
- `email` 在服务端补齐长度（≤100，与库列一致）和格式校验；
  前端 `maxlength` 对直连 API 无效，不能作为校验依据。
- 上述限流阈值都写在 `BlogPublicController` 对应方法的 `@RateLimit` 注解上，改这里即可；
  注解本身（维度、多规则、拦截顺位）的说明见 `designer-framework` 的 `@RateLimit`。

## 「我的评论」：作者自己查看与删除

博客没有读者账号，所以用**浏览器本地随机密钥**识别评论归属，见 `CommenterKeyUtil`：

| 接口 | 说明 |
|------|------|
| `GET /app-api/blog/public/posts/{id}/comments/mine` | 当前文章下自己的**待审**评论 |
| `DELETE /app-api/blog/public/comments/mine/{id}` | 删除自己的评论 |

- 身份通过请求头 **`X-Commenter-Key`** 传递，不走 query/body：
  它等于管理自己评论的凭证，放 URL 里会进访问日志、浏览器历史、Referer。
- 服务端只存密钥的 **SHA-256**（`blog_comment.commenter_key`），库泄露也无法反推密钥。
- ⚠️ **安全关键点**：删除时按「ID + 密钥哈希」匹配，且密钥哈希为空时一律拒绝。
  否则历史数据（`commenter_key = ''`）会被空密钥匹配，任何人都能删。
- 提交评论时密钥**可选**（缺失仍可提交，只是作者无法自行管理），
  但 `mine` / `delete` 接口**必须**提供，且格式须为 16~64 位 `[A-Za-z0-9_-]`。
- 只列 `status=0`：已通过的评论本来就在公开列表里，被拒的不必再展示给作者。
- 清理浏览器数据 / 换设备会拿到新密钥，届时无法再管理旧评论（匿名评论的固有取舍）。

## 评论开关（两级）

评论的开启由**两级开关**共同决定，是「与」关系：

| 层级 | 存储位置 | 配置入口 |
| --- | --- | --- |
| 站点级（总开关） | `blog_setting.comment_enabled` | 后台「站点设置 → 开启评论」 |
| 单篇级 | `blog_post.allow_comment` | 后台文章编辑页「允许评论」 |

- 只有**两者都开启**时才允许提交评论；
- 提示语按层级区分，便于访客判断是站点关了还是这篇关了：
  站点级关闭 → 「站点已关闭评论功能」，单篇级关闭 → 「该文章已关闭评论」；
- 站点级校验**排在验证码之前**（`assertCommentEnabled`），
  站点关闭时立刻拒绝，不会白白消费掉访客的一张验证码；
- **管理员的 `POST /reply` 同样受总开关约束**：前台关闭后整个输入区都不渲染，
  此时允许回复只会造出「看不见的评论」；
- 关闭后**已发布的评论仍然可读**：评论是文章的一部分历史，
  总开关只收起输入区，不把历史评论一并抹掉（前台 `comments.length` 判断）；
- `comment_enabled` 为脏值或缺省时按**开启**处理（`normalizeEnabled`），
  避免一条配置没写好就静默把整站评论关掉。

## 访问统计

前台每次页面访问上报一次埋点，后台「访问统计」页按天/维度聚合出图表。

### 接口

| 接口 | 权限 | 说明 |
|------|------|------|
| `POST /app-api/blog/public/visit` | 无（`@SaIgnore`） | 访客埋点上报，**永远返回成功** |
| `GET /admin-api/blog/visit/overview?days=30` | `blog:visit:query` | 统计概览，一次返回全部图表数据 |

### 埋点只接收路径

`POST /visit` 的请求体只有 `{ "path": "/post/markdown-guide" }`，
**其余字段全部由服务端从请求推导**：

| 字段 | 来源 |
| --- | --- |
| `ip` | `ctx.realIp()`（经可信代理加固，见「客户端 IP 防伪造」） |
| `ua` / `device` | `User-Agent` 请求头 → `UserAgentUtil` 归类 |
| `referer_host` / `referer_type` | `Referer` 请求头 → `RefererUtil` 解析 |
| `ip_location` | `ipRegionResolver.lookup(ip)`，与评论同一套离线解析 |
| `post_id` / `category_id` | 仅详情页，按 slug **反查数据库** |
| `visitor_key` | `SHA-256(ip + "\|" + ua)`，用于 UV 去重 |

这样前端只能说明「看了哪个路径」，无法伪造自己的 IP、来源或归属分类来污染统计。

### 几条刻意的取舍

- **埋点永远返回成功，且失败只记 WARN 日志**（`BlogVisitService#record` 吞掉所有异常）。
  统计是附加能力，让访客因为「统计写失败」看到报错毫无意义；前台也因此可以 fire-and-forget 上报。
- **站内跳转不算来源**：博客页面互相跳转时 `Referer` 就是本站自身，
  照单全收会让「来源 Top」被自家域名霸榜。与站点自身域名一致时归为**直接访问**。
  比对时会统一小写、去 `www.` 前缀并忽略端口。
- **爬虫单列为 `BOT`** 而不是并入「其他」：个人站点的流量里机器人占比往往不低，
  明确标出来才好判断「访问量涨了」是真人还是抓取。
- **不存在的文章按 `other` 记**：`/post/不存在的slug` 反查不到文章就不写 `post_id`，
  避免无效 URL 把某篇文章（或某个分类）的统计刷高。
- **接口路径不算页面**：`/app-api/**`、`/admin-api/**` 会被判为无效路径直接跳过。
  早期实现里缺 `path` 时回退到 `ctx.path()`，结果把上报接口自身的 URL 记成了一次「访问」——
  埋点宁可少记，也不该记错。
- **趋势图补齐空缺日期**：查询结果按天铺平成连续区间、缺的补 0，否则折线会断线。

### 防刷与去重

匿名写接口必须限流，否则脚本可以随意把统计刷爆。都用 Redis 计数
（`RedisCounter#incrWithTtl`，与评论防刷同一套写法；**固定窗口**，锚在窗口内第一次上报）：

| 限制 | 键 | 参数 | 超限行为 |
| --- | --- | --- | --- |
| 同访客同路径去重 | `blog:visit:dedup:{visitorKey}:{path}` | 60 秒 | 静默丢弃 |
| 单 IP 每日上限 | `blog:visit:quota:{ip}` | 3000 条 / 24h | 静默丢弃 |

- **去重先于配额**：刷新、重复上报、前端路由抖动在这一步就被挡掉，不消耗配额。
  `incrWithTtl` 的原子 `incr` 同时充当「是否首次」的判断，无需 `setnx`。
- 超限一律**静默丢弃**（不抛异常）：对访客毫无感知，只是不再计数。

### 阅读数（`view_count`）

阅读数**不由文章详情接口写**，而是搭在访问埋点这条链路上：

| 环节 | 位置 | 行为 |
| --- | --- | --- |
| 计数 | `BlogVisitService#recordRead` | 埋点通过 60 秒去重与配额后，`HINCRBY blog:post:view:pending {postId} 1` |
| 展示 | `BlogPostService#applyPendingViews` | 展示值 = **库中已落地的 + 尚未回写的增量**（列表与详情的口径一致） |
| 回写 | `BlogViewFlushJob` | 每 30 秒把增量合并成一条 `UPDATE blog_post SET view_count = view_count + N` |

这么放有三个好处：

- **读接口不产生写，页面 HTML 也能缓存**。若在 `publicDetailBySlug` 里直接 `view_count + 1`，
  一次廉价 GET 就变成一次数据库写：热文章上有行锁竞争，而且一旦缓存整页，
  阅读数就会按缓存命中次数失真 —— 这也是它导致页面无法缓存的原因。
  挪到浏览器侧后两个问题一起消失：N 次阅读合并成 1 次写，且缓存命中时埋点照常上报。
  nginx 层的 SSR 页面缓存正是靠这一点才敢开（见 `deploy/README.md` 第 9 条）。
- **只有真人算阅读数**。SSR 取数走内网，后端看到的 IP 是 `blog` 容器、UA 是 Node 的 `fetch` ——
  既无法按访客去重，也认不出爬虫，爬虫抓一次阅读数就涨一次。挂在浏览器侧埋点上，
  上面的 60 秒去重与 `UserAgentUtil.BOT` 判定都是白拿的，爬虫（不执行 JS）根本走不到这里。
- **计数口径**：同一访客对同一篇文章 60 秒内只算一次（刷新不叠加），爬虫不计入，
  所以数字会比「按请求数累加」低，但更接近真实阅读。

几个刻意的实现细节：

- **用 Redis Hash 而不是一堆独立 key**：Hash 本身就是「待回写文章 id 的集合」，
  一轮 `HGETALL` 就能取一批，不必 `KEYS blog:post:view:*`（O(N) 且会阻塞 Redis），
  也不用额外维护 dirty set。Redis 有持久化，进程重启后未回写的增量还在。
- **扣减用 `HINCRBY -delta` 而不是清零**：回写期间新到的阅读不会被一并抹掉，留到下一轮。
- **落库失败不扣减**：增量留在 Redis 下轮重试。阅读数宁可晚一点，也不能丢。
- **回写带 Redis 锁**（`blog:post:view:flush:lock`）：多实例部署时只让一个实例落库，
  否则同一批增量会被两个实例各加一次、凭空翻倍。
- **读路径对 Redis 故障降级**：`BlogViewCounter#pendingBatch` 读失败时按「无增量」处理，
  只显示上次回写的旧值 —— 不能因为 Redis 抖动就让文章列表 / 详情打不开。

### 页面类型

`VisitPathUtil` 按 blog-web 的路由规则归类（两边改动需同步，否则会落成 `other`）：

| 路径 | `page_type` |
| --- | --- |
| `/` | `home` |
| `/post/{slug}` | `post` |
| `/category/{slug}` | `category` |
| `/tag/{slug}` | `tag` |
| 其他 | `other` |

### 图表口径

| 图 | 口径 |
| --- | --- |
| PV / UV 趋势 | 按天；UV = 当天 `visitor_key` 去重（IP+UA），换网络或换浏览器会算作新访客 |
| 分类访问分布 | **仅文章详情页**写入 `category_id`，所以反映的是「各分类下文章被访问的量」 |
| 来源 Top | 直接访问的 `host` 为空串，前端展示为「(直接访问)」 |
| 设备占比 | `PC / MOBILE / TABLET / BOT / OTHER`，库里存语言无关的码，展示时在 Java 侧转中文 |
| 访客地域分布 | Top 10 省份；归属地解析不到的不参与排行 |

### 数据保留

访问日志是明细表，会随流量持续增长。内置任务 `blogLogTask.cleanVisitLog(days)` 清理保留期之外的数据，
种子任务默认**暂停**（`sys_job` id=4，每天 3:30，保留 90 天），按需在「系统监控 → 定时任务」里开启：

```
blogLogTask.cleanVisitLog(90)    # 清理 90 天前
blogLogTask.cleanVisitLog(null)  # 用默认保留期
```

### 隐私

日志存了 IP、归属地、UA 与来源，属于个人可识别信息，**只应在自有服务器上自用**：

- 统计接口只返回聚合结果，不对外输出原始 `ip` / `ua`；
- 建议按实际需要调小保留期（改任务参数即可）；
- 若要合规到更严格的程度，可只保留 `ip_location` 而在写入时就把 `ip` / `ua` 置空
  （代价是无法再排查具体访客）。

## 升级已有数据库

`schema.sql` 只在全新初始化时生效。已有库需要手动补列：

### 升级已有数据库：评论总开关

```sql
ALTER TABLE `blog_setting`
    ADD COLUMN `comment_enabled` TINYINT NOT NULL DEFAULT 1
        COMMENT '全局评论总开关：1 开启，0 关闭（单篇仍受 blog_post.allow_comment 约束）'
        AFTER `seo_description`;
```

### 升级已有数据库：评论人匿名密钥

```sql
ALTER TABLE `blog_comment`
    ADD COLUMN `commenter_key` VARCHAR(64) NOT NULL DEFAULT ''
        COMMENT '评论人匿名标识（客户端密钥的 SHA-256）' AFTER `ip`,
    ADD KEY `idx_blog_comment_owner` (`post_id`, `commenter_key`, `status`);
```

### 升级已有数据库：IP 归属地

```sql
ALTER TABLE `blog_comment`
    ADD COLUMN `ip_location` VARCHAR(64) NOT NULL DEFAULT ''
        COMMENT 'IP归属地（省级，如「广东」；内网为「内网」）' AFTER `ip`;
```

存量评论的 `ip_location` 为空串，前台不会显示归属地（`v-if` 兜底）；
若不希望出现新旧不一致，可对历史数据自行补跑归属地解析后再更新。

### 升级已有数据库：评论作者身份与站长署名（防冒充博主）

```sql
ALTER TABLE `blog_comment`
    ADD COLUMN `author_type` TINYINT NOT NULL DEFAULT 0
        COMMENT '作者类型：0访客 1博主（仅服务端写入，前端据此展示「博主」标识，防止冒充）'
        AFTER `nickname`;

ALTER TABLE `blog_setting`
    ADD COLUMN `owner_nickname` VARCHAR(50) NOT NULL DEFAULT '博主'
        COMMENT '站长评论署名（后台回复默认署名，并自动加入保留昵称）'
        AFTER `comment_enabled`,
    ADD COLUMN `reserved_nicknames` VARCHAR(500) NOT NULL DEFAULT '博主,站长,管理员,admin'
        COMMENT '保留昵称（逗号分隔）：访客昵称包含其中任一词即拒绝，后台可配'
        AFTER `owner_nickname`;
```

`ADD COLUMN ... DEFAULT` 会让存量行直接拿到默认名单，不需要额外 `UPDATE`。

**存量数据回填**：历史评论没有身份标记，需要按已有信号推断。**`ip = ''` 是唯一可靠的判据**：

- 后台回复走的是 `BlogCommentService#reply()`，该方法**显式写入 `ip = ""`**（刻意的：
  公开归属地会暴露站长位置），且不落 `commenter_key`；
- 访客评论的 IP 由 `BlogCommentService#submitPublic` 取 `ClientIpUtil.current()` 并落库，
  即使取不到也会写入 `ClientIpUtil.UNKNOWN_IP`（`"unknown"`），**永远不为空串**
  （与限流键同源，避免同一次请求在评论表和限流桶里被算成两个不同的 IP）。

所以 `ip = ''` 只可能由后台回复产生：

```sql
UPDATE `blog_comment` SET `author_type` = 1 WHERE `ip` = '';
```

⚠️ **不要用 `create_by <> ''` 判断**：`create_by` 由 `AutoFillHandler` 填充，登录时有值、
匿名时为空串，看似可用，但一旦历史数据经过脚本导入 / 数据迁移（例如本项目 demo 库中
全部 69 条评论的 `create_by` 都是 `system`），该判据会**把全部访客评论误标为博主**。
同理，也不要按「昵称 = 博主」批量回填：真实访客也可能用过同名昵称。

回填前建议先核对命中条数，确认它们确为站长回复：

```sql
SELECT id, parent_id, nickname FROM `blog_comment` WHERE `ip` = '';
```

标记只影响展示层的「博主」标识：未回填到的历史站长回复会以普通访客样式显示，不影响内容本身。

#### 保留昵称：后台可配 + 「包含」匹配

访客不可占用的昵称 = **后台配置的保留昵称**（`blog_setting.reserved_nicknames`，逗号分隔）
+ **站长署名**（自动追加），由 `NicknameUtil.reserved(configured, ownerNickname)` 统一产出，
按归一化键去重（所以署名就是「博主」时不会重复出现）。

**名单不再硬编码在代码里**：`NicknameUtil.DEFAULT_RESERVED`（`博主` / `站长` / `管理员` / `admin`）
只用于建库默认值与「设置行缺失」时的兜底；后台把它改成什么就以什么为准，
清空即表示不拦截这些词（站长的显式操作不该被代码悄悄覆盖）。
后台入口：站点设置 → 评论 → **保留昵称**（逗号分隔，全角逗号 / 分号 / 换行都能当分隔符，
存库前会被归一化去重回写）。

**匹配是「包含」而不是「等于」**：昵称里出现任意一个保留词即拒绝，
用来挡「博主小号」「我是站长」「admin01」这类夹带式冒充。
- 比较前统一走 `NicknameUtil.key`：NFKC 折叠全角/兼容字符 + 去掉全部空白 + 转小写，
  所以 `ADMIN` / `Admin` / `ａｄｍｉｎ` / `a d m i n` 与 `admin` 同形（这就是「英文忽略大小写」的实现点）；
- 代价是保留词本身若很泛就会被误伤（例如填「站」，连「车站」都拦），
  所以保留词要填完整、有辨识度的词 —— 后台提示语里写明了这一点。

两处消费，同一个来源：

| 用途 | 位置 |
| --- | --- |
| 服务端拦截（权威） | `BlogCommentService#submitPublic` → 命中即拒绝，提示语带上命中的词 |
| 下发给前台做即时提示 | `PublicSiteVO#reservedNicknames`（`/app-api/blog/public/site` 返回） |

之所以下发而不是让前端照抄一份：**两边各留一份名单必然漂移** —— 后端加了词、前端没加，
用户就会填完才被后端打回；反过来前端多拦一个词则属于无谓的误伤。
公开这份名单不构成风险：这些词本来就会通过错误提示暴露，且真正的拦截只在服务端。

名单由 `NicknameUtil`（静态工具）产出而不是让 `BlogSettingService` 注入
`BlogCommentService`，避免后者反向依赖前者的循环依赖；
`BlogSettingService#getReservedNicknames()` 用一次 `get()` 读出全部原始字段，
避免多次查库期间配置被改而拼出不一致的名单（下发给前台的与拦截用的是同一份值）。

### 升级已有数据库：评论回复权限

`data.sql` 只在全新初始化时生效。已有库需补一条菜单权限（`8043`）：

```sql
INSERT INTO `sys_menu`
(`id`,`parent_id`,`menu_name`,`menu_type`,`path`,`component`,`perms`,`icon`,`sort`,`visible`,`status`,`create_by`,`create_time`,`update_by`,`update_time`)
VALUES (8043, 8004, '评论回复', 'F', '', '', 'blog:comment:reply', '', 4, 1, 1, 'system', NOW(), 'system', NOW())
ON DUPLICATE KEY UPDATE `perms` = VALUES(`perms`), `menu_name` = VALUES(`menu_name`);
```

超级管理员（`*`）不受影响；其他角色需在角色授权里勾选该按钮。

### 升级已有数据库：访问统计

建表（与 `sql/init/mysql/blog/schema.sql` 中的定义一致）：

```sql
CREATE TABLE `blog_visit_log` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `visit_date`   DATE         NOT NULL                COMMENT '访问日期（冗余列：按天聚合走索引）',
    `page_type`    VARCHAR(16)  NOT NULL DEFAULT ''     COMMENT '页面类型：home/post/tag/category/other',
    `path`         VARCHAR(255) NOT NULL DEFAULT ''     COMMENT '访问路径（已去 query 与锚点）',
    `post_id`      BIGINT       DEFAULT NULL            COMMENT '文章ID（仅详情页）',
    `category_id`  BIGINT       DEFAULT NULL            COMMENT '文章分类ID（仅详情页）',
    `ip`           VARCHAR(64)  NOT NULL DEFAULT ''     COMMENT '客户端IP',
    `ip_location`  VARCHAR(64)  NOT NULL DEFAULT ''     COMMENT 'IP归属地（省级）',
    `visitor_key`  VARCHAR(64)  NOT NULL DEFAULT ''     COMMENT '访客标识（IP+UA 的 SHA-256）',
    `device`       VARCHAR(16)  NOT NULL DEFAULT ''     COMMENT '设备：PC/MOBILE/TABLET/BOT/OTHER',
    `referer_host` VARCHAR(128) NOT NULL DEFAULT ''     COMMENT '来源域名，空为直接访问',
    `referer_type` VARCHAR(16)  NOT NULL DEFAULT ''     COMMENT '来源类型：direct/search/external',
    `ua`           VARCHAR(255) NOT NULL DEFAULT ''     COMMENT 'User-Agent（截断）',
    `create_by`    VARCHAR(50)  NOT NULL DEFAULT ''     COMMENT '创建人',
    `create_time`  DATETIME     DEFAULT NULL            COMMENT '创建时间',
    `update_by`    VARCHAR(50)  NOT NULL DEFAULT ''     COMMENT '更新人',
    `update_time`  DATETIME     DEFAULT NULL            COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_blog_visit_date` (`visit_date`),
    KEY `idx_blog_visit_visitor` (`visit_date`, `visitor_key`),
    KEY `idx_blog_visit_post` (`post_id`, `visit_date`),
    KEY `idx_blog_visit_category` (`category_id`, `visit_date`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '博客访问日志';
```

菜单（`8008` 目录 + `8062` 查询按钮）与清理任务（`sys_job` id=4）：

```sql
INSERT INTO `sys_menu`
(`id`,`parent_id`,`menu_name`,`menu_type`,`path`,`component`,`perms`,`icon`,`sort`,`visible`,`status`,`create_by`,`create_time`,`update_by`,`update_time`)
VALUES
(8008, 8000, '访问统计', 'C', '/blog/visit', 'blog/visit/index', 'blog:visit:list', 'trending', 1, 1, 1, 'system', NOW(), 'system', NOW()),
(8062, 8008, '访问统计查询', 'F', '', '', 'blog:visit:query', '', 1, 1, 1, 'system', NOW(), 'system', NOW())
ON DUPLICATE KEY UPDATE `menu_name` = VALUES(`menu_name`), `path` = VALUES(`path`),
    `component` = VALUES(`component`), `perms` = VALUES(`perms`), `icon` = VALUES(`icon`), `sort` = VALUES(`sort`);

-- 把「文章管理 → 站点设置」的 sort 从 1..6 改为 2..7，给访问统计让出第一位。
-- 排序是 (parent_id, sort)，直接置 1 会与文章管理并列，显示顺序就不确定了。
UPDATE `sys_menu` SET `sort` = 2 WHERE `id` = 8001;
UPDATE `sys_menu` SET `sort` = 3 WHERE `id` = 8002;
UPDATE `sys_menu` SET `sort` = 4 WHERE `id` = 8003;
UPDATE `sys_menu` SET `sort` = 5 WHERE `id` = 8004;
UPDATE `sys_menu` SET `sort` = 6 WHERE `id` = 8006;
UPDATE `sys_menu` SET `sort` = 7 WHERE `id` = 8007;

INSERT INTO `sys_job`
(`id`,`job_name`,`job_group`,`invoke_target`,`cron_expression`,`misfire_policy`,`concurrent`,`status`,`remark`,`create_by`,`create_time`,`update_by`,`update_time`)
VALUES (4, '清理访问日志', 'SYSTEM', 'blogLogTask.cleanVisitLog(90)', '0 30 3 * * ?', 1, 0, 0,
        '内置：每天 3:30 清理 90 天前的博客访问日志（默认暂停）', 'system', NOW(), 'system', NOW())
ON DUPLICATE KEY UPDATE `invoke_target` = VALUES(`invoke_target`), `cron_expression` = VALUES(`cron_expression`);
```

超级管理员（`*`）不受影响；其他角色需在角色授权里勾选该菜单。

### 示例评论

`sql/init/mysql/blog/data.sql` 的「示例评论」段包含 10 条种子评论（ID 101–110），
随初始化一起写入，覆盖后台「评论管理」的关键场景：

| 场景 | 评论 |
| --- | --- |
| 「回复的回复」深链（显示时压平为 2 层） | 101 → 102 → 103 → 110 |
| 同一顶级下的多条管理员回复 | 101 → 107 |
| 博主回复博主的深链（压平为 2 层） | 108 → 109 |
| 待审状态（用于试审核通过 / 拒绝） | 104、106 |
| 不同 IP 归属地 | 江苏 / 广东 / 浙江 / 北京 / 上海 |

- 时间用 `DATE_SUB(NOW(), ...)` 相对生成，所以**任何时间初始化，看起来都是最近几天的评论**，
  不会随着仓库放置时间变长而变成一堆「几个月前」。
- `commenter_key` 统一为空串：这些是站方种子数据，无人能通过「我的评论」接口认领或删除它们。
- ID 从 101 起是为了与真实评论区分：不需要时一条语句即可清空：

  ```sql
  DELETE FROM `blog_comment` WHERE `id` BETWEEN 101 AND 110;
  ```

### 客户端 IP 防伪造

限流键基于客户端 IP，而 IP 有**两层**含义，安全性完全不同：

- **TCP 源 IP（socket IP）**：伪造不了。要提交一条评论必须先完成 TCP 三次握手，
  而 SYN-ACK 会发往伪造的源地址、攻击者收不到，握手无法完成，HTTP 请求也就发不出去。
- **请求头里的 IP（`X-Forwarded-For` / `X-Real-IP`）**：**可以被随意伪造**。
  Solon 默认的 `HostUtil.getRealIp` 无条件采信这两个头（且 `X-Forwarded-For` 取的是**首段**），
  于是攻击者每次请求换一个头值就能拿到一个全新的限流桶，限流形同虚设。

本项目已在框架层加固（`designer-framework`）：

| 组件 | 作用 |
|---|---|
| `TrustedProxyHostUtil` | 替换 Solon 默认的 `HostUtil`（`HostUtil.globalSet`）。对端 socket IP **不在可信代理网段内则完全忽略请求头**；可信时沿 `X-Forwarded-For` **从右往左**取第一个不可信地址（右侧都是自己的代理）。返回值保证是合法 IP，垃圾值会被跳过 |
| `IpRange` | IPv4 / IPv6 CIDR 匹配。**只解析字面量、绝不触发 DNS**（这些字符串来自攻击者可控的请求头，`InetAddress.getByName` 直连会让对方借构造主机名引发 DNS 查询） |
| `ClientIpConfig` | 读取配置 `designer.client-ip.trusted-proxies`（逗号分隔的 IP / CIDR），启动时全局注册 |

**可信网段默认值**（可在 `app.yml` 或用环境变量 `TRUSTED_PROXIES` 覆盖）：

```yaml
designer:
  client-ip:
    trusted-proxies: "${TRUSTED_PROXIES:127.0.0.1/32, ::1/128, 172.16.0.0/12}"
```

默认对应「nginx 与 server 同机 / 同 compose 网络」：回环 + Docker 默认网桥网段。

⚠️ **两条必须注意的部署事项**：

1. **反向代理不在可信网段内时一定要补上**，否则所有请求都会被当作来自代理，
   真实客户端 IP 会退化成代理地址，限流会误伤全体用户（`ClientIpConfig` 已为此打 WARN 日志）。
2. **可信网段不要配得过宽**：若真实客户端本身来自某个可信网段（例如办公网 `10.x`），
   该网段内的地址就能互相伪装。按需收窄到只包含自己的代理。

配套的部署层加固（见 `deploy/`）：

- `docker-compose.app.yml`：server 端口只绑定 `127.0.0.1`，**不对外发布**。否则攻击者可绕过 nginx
  直连 8080 随意伪造请求头，代码层的防护就形同虚设。
- `nginx/default.conf`：用 `X-Forwarded-For $remote_addr`（覆写）而非 `$proxy_add_x_forwarded_for`
  （追加）。后者会把客户端自带的伪造链一起传给后端。

**实测效果**：修复前，同一来源换 5 个伪造 `X-Real-IP` 连发 5 条评论全部成功、生成 5 个限流桶；
修复后仅第 1 条成功，其余被同一个 30 秒限流拦住，落库的是真实 socket IP。

评论默认 `status=0` 待审，只有审核通过才公开。IP 只是粗粒度防线（NAT/CGNAT 下大量用户共用出口 IP，
会误伤），验证码 + 人工审核才是最终兜底。

### IP 归属地

评论落库时记录 **省级** 归属地（`blog_comment.ip_location`），前台评论列表与后台评论管理都会展示。

- **离线解析**：基于 [ip2region](https://github.com/lionsoul2014/ip2region) 的 xdb 数据文件，
  **不发起任何网络请求**。既不依赖第三方接口可用性，也不会把访客 IP 泄露给外部服务，且没有调用额度限制。
- **数据文件**：`designer-server/src/main/resources/ip2region.xdb`（约 11MB，随应用打包）。
  想单独升级数据而不重新打包时，用配置指向外部文件：

  ```yaml
  designer:
    ip-location:
      xdb-path: "${IP2REGION_XDB_PATH:}"   # 留空 = 用类路径随包数据；外部文件不可读时自动回退
  ```

- **写入时固化**：归属地在提交那一刻解析并存入库，之后更换数据文件也不会改变历史记录。
- **归一化**：`广东省` → `广东`、`北京市` → `北京`、`内蒙古自治区` → `内蒙古`；
  数据文件里省份为占位符 `0` 时退回国家（如 `美国`）。
- **兜底**：内网/保留地址（`127.0.0.1`、`10./172.16./192.168.`、`::1`、CGNAT `100.64/10` 等）直接标为
  **内网**；非法值或公网 IPv6（xdb 为 IPv4 数据集）标为 **未知**。
- **缺文件不阻塞启动**：数据文件缺失或损坏时只打 WARN 日志，归属地统一退化为「未知」。

实现：`IpRegionResolver`（`designer-framework/client`），由 `ClientIpConfig` 注册为 Bean。
