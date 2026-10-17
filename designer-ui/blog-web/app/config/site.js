/**
 * 站点默认配置（仅作为兜底默认值）
 *
 * 站点名称 / Logo / 页脚版权 / 备案号 已改为后台可配（「博客模块 → 站点设置」），
 * 运行时由 src/composables/useSiteSetting.js 拉取并覆盖此处默认值。
 * Logo / 主题色与 admin-web 同步（见 src/config/theme.js）
 */

/** 品牌信息（后端未配置时的兜底值） */
export const brand = {
  /** 站点名称（导航 Logo 旁） */
  name: 'Designer Blog'
}

/** 文章列表页 */
export const blogPage = {
  title: '文章',
  emptyText: '暂无文章',
  pageSize: 5
}

export default {
  brand,
  blogPage
}
