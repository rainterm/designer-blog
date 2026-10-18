# blog-web

博客公开前台，**Nuxt 4 + Naive UI**，SSR 直出，对接 `/app-api/blog/public/*`。

## 为什么是 SSR

SEO 与分享预览是前台选 SSR 而非纯客户端渲染的原因：

1. **搜索引擎收录。** 百度等对 JS 渲染的抓取能力弱，正文全靠 JS 运行时生成的文章常常长时间不被收录。
2. **分享卡片是空的。** 微信 / QQ / Twitter / Facebook / Slack 的爬虫**完全不执行 JS**，
   它们能看到的只有首屏 HTML 里的 `og:` 标签 —— 没有就只剩一张没有标题、没有缩略图的白卡片。

所以 HTML 由服务端渲染：正文、评论、`<title>`、`og:*`、JSON-LD 全部随首屏返回，爬虫零 JS 即可读取。

## 开发

```bash
cd designer-ui/blog-web
pnpm install
pnpm dev            # 默认 http://localhost:3000
```

后端需先跑在 `http://localhost:8080`（见 `NUXT_API_INTERNAL_BASE`）。

```bash
pnpm build          # 产出 .output/（Node 服务，不是静态站点）
pnpm preview        # 用 Node 跑一遍生产产物
```

## 配置

`.env`（本地）/ 容器环境变量：

| 变量 | 作用 |
|------|------|
| `NUXT_API_INTERNAL_BASE` | **仅服务端**：SSR 期间直连后端的绝对地址。本地 `http://localhost:8080`，容器内 `http://server:8080` |
| `NUXT_PUBLIC_APP_TITLE` | 站点兜底名称（后台「站点设置」未配置时用于标题） |
| `NUXT_PUBLIC_SITE_URL` | 站点对外地址，只用于 sitemap 的绝对链接；留空按请求 Host 推导 |

> 浏览器侧**不用**这个 base：公开接口固定走相对路径 `/app-api`，由 nginx 反代。
> 于是同一份产物在本地与容器里都不需要改代码。

## 目录

```
app/
├── api/                 # request.js（$fetch 请求层）+ blog.js（接口清单）
├── components/          # AppHeader / PostCard / Comment* / PaginationBar / ...
├── composables/         # useSiteSetting（站点设置）/ useBlogSeo（页面级 SEO）
├── config/              # theme.js（主题源）/ site.js / brandMark.js
├── layouts/default.vue  # 外壳（页头、页脚、滚动容器、provide blogScrollEl）
├── pages/               # index / post/[slug] / category/[slug] / tag/[slug]
├── plugins/             # naive-ui / blog-config.server / visit-tracker.client / ...
├── error.vue            # 错误页（真 404 / 502，而非「200 + 空页面」）
└── utils/               # format / feedback / commenterKey / visitTracker
server/
├── routes/              # health（容器自检）/ robots.txt / sitemap.xml
└── utils/blogApi.js     # Nitro 侧取数 + 站点 origin 推导
scripts/                 # 构建期同步 tokens.css 与 favicon.svg（见「主题」）
```

## 路由

| 路径 | 接口 |
|------|------|
| `/` | 文章列表（支持 `?page=`、`?q=`） |
| `/post/:slug` | 文章详情 + 评论（不存在时返回 **HTTP 404**） |
| `/category/:slug` | 按分类 |
| `/tag/:slug` | 按标签 |
| `/robots.txt`、`/sitemap.xml` | 动态生成（见「SEO」） |
| `/health` | 容器自检，不碰后端 |

## 取数约定（SSR 的关键）

- **页面数据一律走 `useAsyncData` + `$fetch`**（`app/api/request.js`）。
  服务端取到的数据会进 Nuxt payload，客户端直接复用，因此**不会重复请求**，
  也不会出现「服务端渲染一套、客户端首帧又拉一次」的闪动。
- **请求层必须用 `$fetch` 而不能用 axios**：axios 在 Node 里没有「页面来源」，
  相对路径无从解析。`$fetch` 一套代码两端通用。
- **后端把所有业务异常也返回 HTTP 200**（见 `GlobalExceptionFilter`），
  所以失败判定不能只看状态码，必须检查响应体里的 `code` —— `request.js` 已统一处理，
  并抛出带 `code` 的 `ApiError` 供页面分支（404 / 502）。
- **页面级取数统一传 `{ silent: true }`**：首屏失败应该走错误页，而不是在访客面前弹 toast。
- **客户端专属行为必须显式隔离**：验证码加载、`MdCatalog` 的 DOM 测量、
  `localStorage` 里的评论者密钥、访问埋点，都只在浏览器侧执行
  （`.client.js` 插件 / `onMounted` / `import.meta.client` 判断）。
- **SSR 页面缓存不在这里配 `routeRules`，而是落在 nginx 层**（见 `deploy/README.md` 第 9 条）：
  缓存的意义在于「命中时完全不走 Node」，而 `routeRules` 仍要经过 Node 进程。
  能这么做的前提正是上一条的隔离 —— 评论者密钥、验证码、埋点都在浏览器侧，
  SSR 输出对所有访客一致，不存在缓存串号。代价是评论与站点设置最多滞后 60 秒。
- **`:prefetch="false"`**（见 `PostCard.vue`）：Nuxt 的链接预取会在卡片进入视口时就请求目标页数据，
  首页 5 张卡片就是 5 次额外请求 —— 不值得为「可能点击」提前付出 5 倍开销。

## SEO

`app/composables/useBlogSeo.js` 是页面级 SEO 的唯一入口，一次调用负责：

- `<title>`、`description`、`keywords`（百度仍读）
- `og:title` / `og:description` / `og:type` / `og:url` / `og:image` / `og:site_name` / `og:locale`
- `twitter:card` / `twitter:title` / `twitter:description` / `twitter:image`
- `<link rel="canonical">` —— **保留 `?page=`，剥掉 `?q=`**：
  分页是不同内容、必须收录；站内搜索词组合会造出无穷多可索引 URL，必须收敛
- JSON-LD（文章页是 `BlogPosting`）
- 分享图必须是绝对地址（爬虫不解析相对路径）

`server/routes/` 里还有两个动态端点，属于「让爬虫找得到页面」的另一半：

| 端点 | 说明 |
|------|------|
| `/robots.txt` | 放行全站、屏蔽 `/app-api/`，并指向 sitemap |
| `/sitemap.xml` | 全站 URL 清单（首页 + 分类 + 标签 + 全部已发布文章） |

两者里的绝对地址都由请求的 `Host` / `X-Forwarded-Proto` 推导（nginx 已透传），
所以**改域名不用改代码**，只有「前面又套了一层 CDN 且回源 Host 不是站点域名」时才需要配 `NUXT_PUBLIC_SITE_URL`。

### 真 404 而不是软 404

后端在 slug / 分类不存在时抛 `BizException(ResultCode.NOT_FOUND)`（`code: 404`），
页面据此 `createError({ statusCode: 404 })`，返回**真正的 HTTP 404**。
这一点很重要：客户端渲染的站点这类页面只能回「200 + 空壳」，搜索引擎会把无效 URL 当正常页收录，
既浪费抓取配额又伤害站点质量评分。

## 评论

评论是匿名接口，提交前必须过 **滑块验证码 + 频率限制**（`CommentForm.vue` 负责展示）：

| 接口 | 说明 |
|------|------|
| `POST /captcha/get` | 取一张滑块验证码：底图 + 拼图 + `token` + `secretKey`（**根路径**，不在 `/app-api` 下，dev 由 `nitro.devProxy` 转发） |
| `POST /captcha/check` | 提交拖拽位置，通过则拿到 `captchaVerification` 凭据 |
| `POST /app-api/blog/public/comments` | 提交评论，需带 `captchaVerification`（+ `commenterKey`） |
| `GET /app-api/blog/public/posts/{id}/comments` | 分页拉取已通过评论（`pageNum`/`pageSize`），按顶级评论分页 |
| `GET /app-api/blog/public/posts/{id}/comments/mine` | 我的待审评论（需请求头 `X-Commenter-Key`） |
| `DELETE /app-api/blog/public/comments/{id}/mine` | 删除自己的评论（需请求头 `X-Commenter-Key`） |

- **评论列表也走 SSR**：评论是正文之外最实在的可索引文本，只在客户端拉等于把这块内容整个让给 JS 渲染。
- **滑块组件来自共享模块**：`SliderCaptcha.vue` / 协议工具 / 请求封装都在
  `designer-ui/shared/captcha`（与后台登录**同一份源码**，各自 `alias` 过去，
  见 `nuxt.config.js` 的 `@captcha`）。组件本身不依赖任何 HTTP 客户端，
  传输层由各站注入 —— blog-web 这侧只是一行 `app/utils/captchaApi.js`。
- 滑块**按需展示**：评论表单初始不渲染、也就不请求验证码；首次点击「提交评论」且昵称/内容校验通过后，
  才展开面板（`captchaVisible`）。拖到对齐就**自动提交**，不必再点一次按钮。
- 凭据**一次性**：`/captcha/check` 得到的 `captchaVerification` 被服务端消费后即失效，
  所以提交失败要清掉它并重拖；但 **429 除外** —— 那是被限流拦在业务代码之外，
  凭据根本没被消费，清掉只会让人白拖一次。这条分支写在 `CommentForm.vue#doSubmit` 里。
- 面板内有「换一张」可手动刷新；拖错会自动换一张并抖动提示
  （服务端判定失败后这张就作废了，原地重试必然继续失败）。
- **验证码与提交都必须在浏览器侧发起**：它们依赖真实客户端 IP 做限流，
  若挪到 SSR 期间执行，后端看到的会是 `blog` 容器的 IP，限流就形同虚设。
- 被限流时后端返回具体原因（「评论太频繁了」/「24 小时内评论条数已达上限」），由 `request.js` 统一弹出。
- **回复层级压平为 2 层**：回复的回复统一压平在第 2 层，标题显示「某某 回复 某某」（`replyToNickname`）。
- `CommentList.vue` 用**迭代**方式摊平评论树（而非递归），避免深层回复链爆栈。
- **评论开关两级**：站点总开关（`site.commentEnabled`）与单篇开关（`post.allowComment`）是「与」关系。
  关闭时只收起输入区与「回复」按钮，**已发布的评论仍可阅读**；提示语区分层级。
- **博主身份认标记不认昵称**：`CommentList.vue` 的「博主」标识只看服务端下发的 `authorType`（1 为博主），
  绝不根据昵称文本判断。后端在提交时把访客评论强制为 `author_type=0`，只有后台回复写 1，
  所以访客把昵称填成「博主」也不会带标识。
- **保留昵称**：由后台「站点设置 → 保留昵称」（`blog_setting.reserved_nicknames`，逗号分隔）配置，
  站长署名会**自动**加入，不必重复填写。匹配是**包含**：昵称里出现任一词即拒绝，
  能拦「博主小号」「我是站长」「admin01」这类夹带式冒充。
  比较前 NFKC 折叠 + 去全部空白 + 转小写，所以 `ADMIN` / `Admin` / `ａｄｍｉｎ` / `a d m i n` 与 `admin` 同形。
- **名单由后端下发，前端不维护副本**：`/site` 返回 `reservedNicknames`（后台配置 + 站长署名，已去重），
  与后端拦截用的是同一份 `NicknameUtil.reserved`；`app/utils/nickname.js` 只负责按同一套归一化规则做「包含」比对。
  两边各留一份名单最容易漂移——后端加了词、前端没加，用户填完才被打回。
  该名单为空时（`/site` 请求失败）退回 `nickname.js` 里的兜底常量，仅作提示、不影响安全。
  归一化挡不住异体字，故这里只用于减少视觉混淆，真正拦截在服务端。

### 我的评论（`MyCommentPanel.vue`）

评论默认待审，通过前不会出现在公开列表里。为让作者确认提交成功并撤回，评论框下方内嵌「我的评论（审核中）」面板：

- 匿名身份由 `app/utils/commenterKey.js` 生成并存在 `localStorage`（`blog:commenterKey`），
  提交评论时随表单上传；请求 `mine` / `delete` 接口时放在 `X-Commenter-Key` 请求头。
- 面板只列**待审**评论（`status=0`）；审核通过后自动从面板消失、出现在公开列表。
- 删除有二次确认，删除后重新拉取列表；列表为空时整块隐藏。
- `localStorage` 不可用（隐私模式等）时退回内存态密钥，当次会话仍可正常评论。
- 这一整块**只在客户端渲染**：它依赖 `localStorage`，服务端没有对应状态。

服务端实现见 `designer-module-blog` 的 `BlogCommentService`（身份识别用 `CommenterKeyUtil`）。

## 访问埋点

路由切换时（`app/plugins/visit-tracker.client.js`）调用 `reportVisit(path)` 向
`POST /app-api/blog/public/visit` 上报一次访问，请求体只有路径：

```js
reportVisit('/post/markdown-guide')   // app/utils/visitTracker.js
```

- **只在客户端上报**：服务端上报会把所有访客的 IP 都写成容器 IP，统计直接失真。
- **刻意不走 `@/api/request`**：那个请求层会在任何失败时弹 `message.error`。
  埋点是附加能力，访客不该因为「统计没上报成功」看到报错。因此这里自建最小实现
  （超时 5s），静默吞掉所有异常、返回的 Promise **永不 reject**。
- 只上报路径，**IP / UA / 来源 / 设备 / 页面归属分类全部由服务端从请求推导**，前端无法伪造统计。
- 服务端对「同一访客 + 同一路径」有 60 秒去重窗口，刷新页面不会重复计数。
- 后台「博客模块 → 访问统计」查看聚合图表。

## 正文渲染

文章详情页的正文直接用 `md-editor-v3` 的 `MdPreview` 渲染，与后台文章编辑器的预览
**同一套渲染器与预览主题**，代码块、复制按钮、折叠、图片点击放大均由组件提供。

关键点：`MdPreview` 的正文是在**渲染函数里**产出的，所以它会随服务端渲染直接进 HTML
——这是「爬虫能读到正文」的实现方式，不需要额外接一套 markdown-it。

站点品牌化只通过覆盖预览主题的设计变量实现（`app/assets/styles/article.css` 里的 `--md-theme-*`）。
不要在正文样式里写 `pre { ... !important }` 之类的结构覆盖：
代码块头部/圆角/内边距由主题结构规则控制，硬覆盖会把头部打散
（历史问题：正文出现裸奔的「▼ js 复制代码」）。

## 文章大纲

详情页左列的大纲用 `MdPreview` 配套的 `MdCatalog`，`editor-id` 与 `MdPreview` 的 `id` 保持一致。

- 滚动容器是 `layouts/default.vue` 的 `.blog-scroll`，通过 `provide('blogScrollEl')` 注入给页面。
- 标题列表来自 `MdPreview` 的 `onGetCatalog` 回调，为空时自动隐藏左列大纲。
- **整块包在 `<ClientOnly>` 里**：`MdCatalog` 依赖真实 DOM 测量，SSR 无意义。
  包起来后服务端与客户端首帧都是「没有大纲」，不存在 hydration 不一致。
- 大纲不参与正文布局：绝对定位挂到正文左侧留白里，所以正文宽度与没有大纲时完全一致。

## 主题

`app/config/theme.js` 是配色/圆角的**唯一来源**。构建期由 `scripts/syncThemeTokens.js`
生成 `app/assets/styles/tokens.css`、由 `scripts/syncBrandMark.js` 生成 `public/favicon.svg`；
两者都在 `nuxt.config.js` 里同步执行，保证早于 Vite / Nitro 落盘。

之所以生成文件而不是运行时注入：CSS 变量必须在**首屏样式表**里就位，
否则会出现「先按默认色渲染、再被 JS 改成品牌色」的闪烁（FOUC），
SSR 场景下还会变成 hydration 样式不一致。

改主题（默认色板）只需改 `theme.js` —— dev 下由 `builder:watch` 自动重建，无需重启。

### 站点主题色（后台可配）

除内置默认色板外，站长可在后台「博客 → 站点设置 → 站点主题色」改站点品牌配色。
它来自公开接口 `/app-api/blog/public/site` 的 `themeColor`，与其他站点设置一起在
`app/plugins/blog-config.server.js` 写进 `useState`，所以 SSR 期间就已就绪。

- 覆盖方式：`AppShell.vue` 用 `theme.js` 的 `buildColors` 从主色派生整套品牌色
  （hover / pressed / light / soft / 渐变等，算法与 admin-web 一致），再把 CSS 变量
  以内联 `style` 写到内容容器 `.app-view` 上；Naive UI 的 `theme-overrides` 同步换一份。
- 为什么是内联变量而不是改 `tokens.css`：变量随 SSR 首屏 HTML 一起下发（爬虫与访客
  拿到同一份，无水合差异），且内联变量是「更近的祖先」，天然胜过 `:root`，
  不必纠结样式顺序。
- 未配置 / 非法 / **等于内置默认**时都不覆盖，直接用 `tokens.css` 的默认色板：
  派生公式与手绘默认色板并非逐位相同，覆盖会让默认观感发生肉眼可见的变化。

## Naive UI 的 SSR 样式

Naive UI 走 `css-render` 在运行时注入样式。服务端渲染时这些样式默认不会被收集，
首屏就会出现「无样式的裸 HTML」。解法见 `app/plugins/naive-ui-styles.server.js`：
挂到 Unhead 的 `ssr:render` 事件上收集并注入 `<head>`（依赖 `@css-render/vue3-ssr`）。

## API 前缀约定

| 端 | 前缀 |
|----|------|
| 管理后台 | `/admin-api/{模块}/...` |
| 公开前台 | `/app-api/{模块}/...` |

博客站点完全不暴露 `/admin-api`（nginx 显式返回 404）。封面 / 正文图片等前台资源必须放在
**公开**存储配置下：这类配置下发的 URL 是 `/app-api/system/file/download/**`，与其它公开接口
共用同一条 `/app-api` 代理（dev 见 `nuxt.config.js` 的 `devProxy`，生产见 nginx 博客 server 块）。
私有存储配置下发的 URL 是 `/admin-api/system/file/download/**`，需要后台登录态，前台取不到。

## 部署

见 `deploy/README.md`。简要地说：`pnpm build` 产出 `.output/`，
由 `deploy/blog/Dockerfile` 打包成 Node 容器（`:3000`，不对外发布端口），
入口 nginx 把博客域名反代到它。
