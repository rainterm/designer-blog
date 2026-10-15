import { fetchSiteSetting, fetchLinks } from '@/api/blog'
import { useBlogLinks, useSiteSetting, mergeSiteSetting } from '@/composables/useSiteSetting'

/**
 * 站点级配置的「首屏数据」：站点设置 + 页脚/顶栏外链。
 *
 * <p>只用 `.server` 后缀是刻意的：服务端取一次、写进 `useState`，值随 payload
 * 一起发给客户端，客户端不再重复请求，水合后立刻就是正确内容。</p>
 *
 * <p>站点配置必须在渲染前就绪：若等客户端挂载后再拉，首屏只能是「默认站点名 +
 * 空版权」，站点名会在页面出现后再跳变一次 —— 而爬虫抓到的那一帧恰恰是错的。
 * 写进 `useState` 后，服务端渲染前就拿到了，爬虫和访客看到的都是最终值。</p>
 *
 * <p>失败一律静默降级为默认值：站点配置缺失不该让整个站点打不开。</p>
 */
export default defineNuxtPlugin(async () => {
  const site = useSiteSetting()
  const links = useBlogLinks()

  const [siteResult, linksResult] = await Promise.allSettled([
    fetchSiteSetting({ silent: true }),
    fetchLinks({ silent: true })
  ])

  if (siteResult.status === 'fulfilled') {
    mergeSiteSetting(site.value, siteResult.value?.data)
  }
  if (linksResult.status === 'fulfilled') {
    links.value = linksResult.value?.data || []
  }
})
