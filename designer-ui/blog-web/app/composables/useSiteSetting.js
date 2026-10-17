import { fetchSiteSetting } from '@/api/blog'
import { brand } from '@/config/site'
import { colors } from '@/config/theme'

/**
 * 站点设置（后台可配）：站点名称 / 副标题 / Logo / 主题色 / 页脚版权 / 备案号 / SEO 默认值 / 评论总开关。
 *
 * <p>这里从「模块级 reactive 单例」换成了 `useState`，原因全是 SSR 带来的：</p>
 *
 * <p>1. 模块级单例在 Node 进程里只创建一次、被所有请求共享。单看站点设置（人人相同）
 * 似乎无害，但它同时是被用户数据之外的请求路径改写的 —— 一旦某次请求拿到的是
 * 半更新状态，就会渲染给另一个访客。</p>
 *
 * <p>2. 更致命的是水合：服务端用「已拉到配置」的 state 渲染出 HTML，客户端水合时
 * 模块单例又回到 `brand.name` 这份初始值，两端首帧不一致，Vue 会报 hydration
 * mismatch 并把整棵树重新渲染 —— 白屏闪烁，还会丢掉 SSR 的收益。</p>
 *
 * <p>`useState` 的值会随 Nuxt payload 序列化给客户端，两侧首帧完全一致。
 * 数据由 `app/plugins/blog-config.server.js` 在服务端填充。</p>
 */
export function siteDefaults() {
  return {
    siteName: brand.name,
    siteSubtitle: '',
    logoUrl: '',
    logoRadius: 6,
    /** 站点主题色（后台可配），默认与内置色板一致 → 不触发覆盖 */
    themeColor: colors.primary,
    /**
     * 页脚版权：默认留空，后台没填就不显示。
     *
     * <p>不要在这里拼一个「© 年份 + 站点名」的兜底行 —— 版权方由站长决定，
     * 猜错了比不显示更糟，而且会让人以为后台清空没生效。</p>
     */
    copyright: '',
    icp: '',
    icpUrl: '',
    seoKeywords: '',
    seoDescription: '',
    /** 全局评论总开关：1 开启，0 关闭（与单篇的 allowComment 是「与」关系） */
    commentEnabled: 1,
    /** 站长评论署名（保留昵称）：仅用于占位等展示 */
    ownerNickname: '',
    /**
     * 保留昵称名单（内置 + 站长署名），由后端下发。
     *
     * <p>前台不再自己维护一份：名单是服务端规则的一部分，前端照抄迟早会漂移。
     * 空的默认值表示「后端还没返回」，此时 `utils/nickname.js` 会退回到内置兜底名单。</p>
     */
    reservedNicknames: []
  }
}

/** 站点设置（响应式；模板里可直接 `site.xxx`，脚本里用 `site.value.xxx`） */
export function useSiteSetting() {
  return useState('blog:site', siteDefaults)
}

/** 站点默认链接（社交/外链），同样由服务端插件填充 */
export function useBlogLinks() {
  return useState('blog:links', () => [])
}

/** SEO 描述兜底：默认描述 → 副标题 → 空 */
export function siteSeoDescription(siteValue) {
  const s = siteValue || useSiteSetting().value
  return s.seoDescription || s.siteSubtitle || ''
}

/**
 * 合并后端配置：空值保留本地默认。
 * 0 是合法值（直角 Logo 圆角、关闭评论），所以用 `!= null` 判断而不是真值判断。
 */
export function mergeSiteSetting(target, data) {
  if (!target || !data) return target
  const textKeys = ['siteName', 'siteSubtitle', 'logoUrl', 'themeColor', 'copyright', 'icp', 'icpUrl', 'seoKeywords', 'seoDescription', 'ownerNickname']
  textKeys.forEach((key) => {
    if (data[key]) target[key] = data[key]
  })
  const nullableKeys = ['logoRadius', 'commentEnabled']
  nullableKeys.forEach((key) => {
    if (data[key] != null) target[key] = data[key]
  })
  // 数组单独处理：空数组视为「后端未下发」，保留本地兜底（utils/nickname.js 另有内置兜底）
  if (Array.isArray(data.reservedNicknames) && data.reservedNicknames.length) {
    target.reservedNicknames = data.reservedNicknames
  }
  return target
}
