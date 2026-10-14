# blog-web

博客公开前台（Vue3 + Vite + Naive UI），对接 `/app-api/blog/public/*`。

## 开发

```bash
cd designer-ui/blog-web
npm install
npm run dev
```

端口 `5174`，`/app-api` 代理到 `http://localhost:8080`。

## 配置

| 文件 | 作用 |
|------|------|
| `src/config/theme.js` | 颜色 / 圆角 / Naive 主题（与 admin-web 同步） |
| `src/config/site.js` | 品牌名、页脚、列表文案 |
| `.env` | `VITE_APP_TITLE` 浏览器标题 |

## 路由

| 路径 | 接口 |
|------|------|
| `/` | 文章列表 |
| `/post/:slug` | 文章详情 + 评论 |
| `/category/:slug` | 按分类 |
| `/tag/:slug` | 按标签 |

## 评论

评论是匿名接口，提交前必须过 **验证码 + 频率限制**（`CommentForm.vue` 负责展示）：

| 接口 | 说明 |
|------|------|
| `GET /app-api/blog/public/comment/captcha` | 下发图形验证码 `{ uuid, img }`，图片为 base64 data URL |
| `POST /app-api/blog/public/comments` | 提交评论，需带 `captchaUuid` + `captchaCode`（+ `commenterKey`） |
| `GET /app-api/blog/public/posts/{id}/comments` | 分页拉取已通过评论（`pageNum`/`pageSize`），按顶级评论分页 |
| `GET /app-api/blog/public/posts/{id}/comments/mine` | 我的待审评论（需请求头 `X-Commenter-Key`） |
| `DELETE /app-api/blog/public/comments/mine/{id}` | 删除自己的评论（需请求头 `X-Commenter-Key`） |

- 验证码**一次性**：不论提交成功或失败，前端都会自动换一张（`loadCaptcha`）。
- 点击验证码图片可手动刷新。
- 被限流时后端返回具体原因（「评论太频繁了」/「24 小时内评论条数已达上限」），
  由 `src/api/request.js` 的拦截器统一弹出。
- **回复层级压平为 2 层**：后端不再限制回复深度，回复的回复统一压平在第 2 层，
  标题显示「某某 回复 某某」（`replyToNickname`）。
- `CommentList.vue` 用**迭代**方式摊平评论树（而非递归），避免深层回复链爆栈。
- **评论开关两级**：站点总开关（`site.commentEnabled`，后台「站点设置」）与单篇开关
  （`post.allowComment`）是「与」关系，两者都开启才渲染输入区。
  关闭时只收起输入区与「回复」按钮，**已发布的评论仍可阅读**；
  提示语区分层级：「站点已关闭评论功能」/「该文章已关闭评论」。

### 我的评论（`MyCommentPanel.vue`）

评论默认待审，通过前不会出现在公开列表里。为让作者确认提交成功并撤回，
评论框下方内嵌「我的评论（审核中）」面板：

- 匿名身份由 `src/utils/commenterKey.js` 生成并存在 `localStorage`（`blog:commenterKey`），
  提交评论时随表单上传；请求 `mine` / `delete` 接口时放在 `X-Commenter-Key` 请求头。
- 面板只列**待审**评论，为 `status=0`；审核通过后自动从面板消失、出现在公开列表。
- 删除有二次确认，删除后重新拉取列表；列表为空时整块隐藏。
- `localStorage` 不可用（隐私模式等）时退回内存态密钥，当次会话仍可正常评论。

服务端实现见 `designer-module-blog` 的 `BlogCommentGuard`。

## 访问埋点

路由切换时（`router/index.js` 的 `afterEach`）调用 `reportVisit(path)` 向
`POST /app-api/blog/public/visit` 上报一次访问，请求体只有路径：

```js
reportVisit('/post/markdown-guide')   // src/utils/visitTracker.js
```

- **刻意不使用 `@/api/request`**：那个实例的响应拦截器会在任何失败时弹 `message.error`。
  埋点是附加能力，访客不该因为「统计没上报成功」看到报错。因此这里自建一个最小 axios 实例
  （超时 5s），并静默吞掉所有异常、返回的 Promise **永不 reject**，调用处无需 catch。
- 只上报路径，**IP / UA / 来源 / 设备 / 页面归属分类全部由服务端从请求推导**，
  前端无法伪造统计（详见 `designer-module-blog` 的「访问统计」）。
- 服务端对「同一访客 + 同一路径」有 60 秒去重窗口，所以刷新页面、重复上报不会重复计数；
  上报失败也不影响任何页面功能。
- 后台「博客模块 → 访问统计」查看聚合图表。

## 正文渲染

文章详情页的正文直接用 `md-editor-v3` 的 `MdPreview` 渲染（`src/views/Post.vue`），
与后台文章编辑器的预览**同一套渲染器与预览主题**，代码块、复制按钮、折叠、图片点击放大均由组件提供。

站点品牌化只通过覆盖预览主题的设计变量实现（`src/styles/article.css` 里的 `--md-theme-*`）。
不要在正文样式里写 `pre { ... !important }` 之类的结构覆盖：代码块头部/圆角/内边距由主题结构规则控制，
硬覆盖会把头部打散（历史问题：正文出现裸奔的「▼ js 复制代码」）。

若需要整块样式与 `MdEditor` 完全一致，改 `src/config/theme.js` 的配色变量即可（两侧共用）。

## 文章大纲

详情页左列的大纲用 `md-editor-v3` 的 `MdCatalog`（`src/views/Post.vue`）渲染，
`editor-id` 与 `MdPreview` 的 `id` 保持一致，滚动高亮、点击跳转由组件自身完成。

- 滚动容器是 `BlogLayout` 的 `.blog-scroll`：通过 `provide('blogScrollEl')` 注入给页面，
  并给 `.blog-scroll` 加 `position: relative`（`MdCatalog` 计算跳转偏移要求滚动容器是定位元素）。
- 标题列表来自 `MdPreview` 的 `onGetCatalog` 回调，为空时自动隐藏左列大纲。
- 大纲不参与正文布局：用绝对定位挂到正文左侧的页面留白里（`right: calc(100% + 20px)`），
  所以正文宽度与没有大纲时完全一致，不会被挤占。
- 视口 ≥1320px（`.`page-container` 最大宽度 920px，两侧留白足够）才显示大纲；更窄时隐藏，正文居中占满。

## API 前缀约定

| 端 | 前缀 |
|----|------|
| 管理后台 | `/admin-api/{模块}/...` |
| 公开前台 | `/app-api/{模块}/...` |
