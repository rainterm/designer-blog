# 博客前端模块

对应后端 `designer-module-blog`（`blog-api` + `blog-service`）。

| 页面 | component |
|---|---|
| 文章列表 | `blog/post/index` |
| 文章编辑 | `blog/post/edit` |
| 分类 | `blog/category/index` |
| 标签 | `blog/tag/index` |
| 评论 | `blog/comment/index` |
| 链接 | `blog/link/index` |
| 站点设置 | `blog/setting/index` |
| 访问统计 | `blog/visit/index` |

## 文章编辑页说明

`blog/post/edit` 是**独立全屏页**，不挂在 `RootLayout` 下（无侧栏 / 顶栏 / 标签页）：

- 路由在前端常量路由中声明（`src/router/index.js` 的 `/blog/post/edit`），不再由后端菜单 `component` 注册；
- 列表页「写文章 / 编辑」通过 `window.open(router.resolve(...).href, '_blank')` 在新标签打开；
- 页面内「返回」与保存成功后调用 `window.close()` 关闭标签，直接输入 URL 打开时回退到 `/blog/post`；
- 因为绕过了菜单授权，路由上用 `meta.authPerm` 声明所需权限，守卫校验失败会跳到 `/403`。

## 评论开关

评论的开启是两级「与」关系，两级都能在本后台设置：

| 层级 | 开关位置 | 字段 |
| --- | --- | --- |
| 站点级（总开关） | 站点设置 → 开启评论 | `blog_setting.comment_enabled` |
| 单篇级 | 文章编辑 → 允许评论 | `blog_post.allow_comment` |

- 站点设置页的开关带说明文案：关闭后前台不再显示输入框，且单篇的「允许评论」不再生效；
- 评论列表页会额外拉一次站点设置（`loadCommentSwitch`），
  总开关关闭时「回复」按钮**直接置灰**并提示「站点已关闭评论功能」，
  避免点下去才被后端拒绝；拉取失败按开启处理，不阻塞列表（后端仍会兜底校验）。

## 访问统计

`blog/visit` 是只读报表页，数据来自前台埋点（见 `designer-module-blog` 的「访问统计」）。

- 顶部 4 张概览卡（区间 PV / 区间 UV / 今日 PV / 今日 UV）+ 5 个 echarts 图：
  访问趋势（折线，PV/UV 双系列）、分类分布（环形）、设备占比（环形）、来源 Top（横向条形）、地域分布（横向条形）。
- 区间切换（近 7 / 30 / 90 天）与「刷新」都走同一个 `getVisitOverview({ days })`，
  后端一次返回全部图表数据：统计页是多图并存，拆成多个接口会让「同一时间窗口」难以保证一致。
- **echarts 按需引入**（`echarts/core` + `LineChart`/`PieChart`/`BarChart` + `CanvasRenderer` +
  `Grid/Tooltip/Legend`），不引全量包；首色取自 CSS 变量 `--color-primary`，跟随主题色。
- 容器 `v-show` 隐藏时尺寸为 0，所以只对**有数据**的图 `echarts.init`（`renderCharts`），
  无数据时渲染 `n-empty`；`resize` 时统一 `resize()`，卸载时 `dispose()`。
- 来源图里 `host` 为空串代表直接访问，展示为「(直接访问)」；
  tooltip 会补上来源类型（直接访问 / 搜索引擎 / 外链），便于判断流量构成。
- 口径提示直接写在图上（「仅统计文章详情页」「站内跳转不计入来源」），
  避免看到分类分布占比之和不是 100% 时误以为是 bug。
