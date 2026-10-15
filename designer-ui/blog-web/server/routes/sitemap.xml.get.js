/**
 * 动态 sitemap.xml。
 *
 * <p>为什么必须有：SSR 解决了「爬虫能读到内容」，但没解决「爬虫能找到页面」。
 * 纯靠站内链接与自然抓取的发现速度很慢，新文章可能要等很久才被收录。
 * sitemap.xml 是主动把全站 URL 一次性交出去，是博客类站点最快的收录通道。</p>
 *
 * <p>不缓存渲染结果，只靠 `cache-control` 让爬虫/CDN 端缓存：
 * 站点文章量在单页 500 以内的规模，随时直查数据库是最简单也最不会出错的做法。</p>
 */

/** 后端的单页上限（见 PageQuery.getPageSize），一次取满减少往返 */
const PAGE_SIZE = 500
/** 翻页安全上限：即使接口返回异常也保证 sitemap 生成不会无限循环 */
const MAX_PAGES = 10

export default defineEventHandler(async (event) => {
  const origin = resolveSiteOrigin(event)

  // 首页永远是必收录项
  const urls = [{ loc: `${origin}/`, changefreq: 'daily', priority: '1.0' }]

  try {
    const [categories, tags, posts] = await Promise.all([
      fetchPublicBlog(event, '/app-api/blog/public/categories'),
      fetchPublicBlog(event, '/app-api/blog/public/tags'),
      fetchAllPosts(event)
    ])

    for (const category of categories || []) {
      urls.push({
        loc: `${origin}/category/${encodeURIComponent(category.slug)}`,
        changefreq: 'weekly',
        priority: '0.6'
      })
    }

    for (const tag of tags || []) {
      urls.push({
        loc: `${origin}/tag/${encodeURIComponent(tag.slug)}`,
        changefreq: 'weekly',
        priority: '0.4'
      })
    }

    for (const post of posts) {
      urls.push({
        loc: `${origin}/post/${encodeURIComponent(post.slug)}`,
        lastmod: toDate(post.publishedAt),
        changefreq: 'monthly',
        priority: '0.8'
      })
    }
  } catch (error) {
    // 后端抖动时不要抛 500：爬虫拿到 500 会降低整个站点的抓取频率，
    // 而「只含首页的 sitemap」最坏也只是少交几个 URL，不会让已收录的页面失效。
    console.error('[sitemap] 取数失败，降级为仅返回首页：', error)
  }

  setHeader(event, 'content-type', 'application/xml; charset=utf-8')
  setHeader(event, 'cache-control', 'public, max-age=1800')

  return [
    '<?xml version="1.0" encoding="UTF-8"?>',
    '<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">',
    ...urls.map(toUrlTag),
    '</urlset>',
    ''
  ].join('\n')
})

/** 分页取全部已发布文章；`publishedAt` 降序，取到不满一页即结束 */
async function fetchAllPosts(event) {
  const all = []
  for (let pageNum = 1; pageNum <= MAX_PAGES; pageNum++) {
    const page = await fetchPublicBlog(event, '/app-api/blog/public/posts', {
      pageNum,
      pageSize: PAGE_SIZE
    })
    const records = page?.records || []
    all.push(...records)
    if (records.length < PAGE_SIZE) break
  }
  return all
}

/**
 * `<lastmod>` 只精确到日期。
 *
 * 后端给的是 `yyyy-MM-dd HH:mm:ss`，补成带时区的完整时间还要假设服务器时区；
 * 而 sitemap 里 `<lastmod>` 本来就是个「变更新鲜度」提示，日期粒度完全够用，
 * 也避免了时区写错导致的时间倒退。
 */
function toDate(value) {
  const matched = /^(\d{4}-\d{2}-\d{2})/.exec(String(value || ''))
  return matched ? matched[1] : undefined
}

function toUrlTag({ loc, lastmod, changefreq, priority }) {
  const lines = [`  <url>`, `    <loc>${escapeXml(loc)}</loc>`]
  if (lastmod) lines.push(`    <lastmod>${lastmod}</lastmod>`)
  if (changefreq) lines.push(`    <changefreq>${changefreq}</changefreq>`)
  if (priority) lines.push(`    <priority>${priority}</priority>`)
  lines.push('  </url>')
  return lines.join('\n')
}

function escapeXml(value) {
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;')
}
