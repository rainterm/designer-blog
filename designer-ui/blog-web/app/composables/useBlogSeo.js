import { useSiteSetting } from '@/composables/useSiteSetting'

/**
 * 页面级 SEO。
 *
 * <p>交给 unhead 在服务端渲染进 HTML，而不是在客户端挂载后用 `document.head`
 * 手写 meta —— 后者只有跑完 JS 的爬虫才看得到。而社交分享爬虫（微信、QQ、Twitter、
 * Facebook、Slack）**完全不执行 JS**，它们能拿到的唯一信息就是首屏 HTML 里的
 * `og:` 标签。没有这些，分享出去就是一张没有标题、没有缩略图的白卡片。</p>
 *
 * @param {object} [options]
 * @param {string} [options.title]      完整标题（与 pageTitle 二选一，优先级更高）
 * @param {string} [options.pageTitle]  页面名，自动拼成「页面名 · 站点名」
 * @param {string} [options.description] 页面描述；缺省回退站点默认描述 / 副标题
 * @param {string} [options.image]      分享图；缺省回退站点 Logo
 * @param {'website'|'article'} [options.type] og:type
 * @param {boolean} [options.noindex]   是否禁止索引（如站内搜索结果页）
 * @param {object|object[]} [options.jsonLd] 结构化数据（JSON-LD）
 */
export function useBlogSeo(options = {}) {
  const site = useSiteSetting()
  const route = useRoute()
  // 这几个组合函数都依赖当前 Nuxt 实例，必须在 setup 阶段先取出来，
  // 不能塞进 computed 的 getter —— 那里可能已经脱离了组件上下文。
  const config = useRuntimeConfig()
  const requestUrl = useRequestURL()

  const siteName = computed(() => site.value.siteName || config.public.appTitle)

  // 选项都支持 ref / getter：页码、搜索词这类值会随客户端路由变化，
  // 只在 setup 时取一次的话，标题与 canonical 会永远停在第 1 页。
  const title = computed(() => {
    const custom = toValue(options.title)
    if (custom) return custom
    const page = toValue(options.pageTitle)
    if (page) return `${page} · ${siteName.value}`
    return siteName.value
  })

  const description = computed(
    () =>
      toValue(options.description) ||
      site.value.seoDescription ||
      site.value.siteSubtitle ||
      ''
  )

  const image = computed(() =>
    absoluteUrl(toValue(options.image) || site.value.logoUrl, requestUrl.origin)
  )

  /**
   * 规范链接：去掉 `?q=` 这类站内搜索参数。
   * 不这么做的话，同一个列表页会因为关键词组合产生无数个可索引 URL。
   * 但分页要保留 —— 翻页是真实的不同内容，砍掉会让第 2 页之后的文章失去收录入口。
   */
  const canonical = computed(() => {
    const params = new URLSearchParams()
    if (route.query.page) params.set('page', String(route.query.page))
    const qs = params.toString()
    return `${requestUrl.origin}${route.path}${qs ? `?${qs}` : ''}`
  })

  useSeoMeta({
    title,
    description,
    // keywords 对 Google 早已无效，但百度仍在读，故保留（站点设置里可配）
    keywords: () => site.value.seoKeywords || undefined,
    ogTitle: title,
    ogDescription: description,
    ogType: () => toValue(options.type) || 'website',
    ogSiteName: siteName,
    ogUrl: canonical,
    ogImage: image,
    ogLocale: 'zh_CN',
    twitterCard: () => (image.value ? 'summary_large_image' : 'summary'),
    twitterTitle: title,
    twitterDescription: description,
    twitterImage: image
  })

  useHead({
    link: [{ rel: 'canonical', href: canonical }],
    meta: computed(() =>
      toValue(options.noindex) ? [{ name: 'robots', content: 'noindex, follow' }] : []
    ),
    script: computed(() => {
      const jsonLd = toValue(options.jsonLd)
      return jsonLd ? [{ type: 'application/ld+json', innerHTML: JSON.stringify(jsonLd) }] : []
    })
  })
}

/** 分享图必须是绝对地址：爬虫不解析相对路径 */
function absoluteUrl(url, origin) {
  if (!url) return ''
  if (/^https?:\/\//i.test(url)) return url
  return `${origin}${url.startsWith('/') ? '' : '/'}${url}`
}
