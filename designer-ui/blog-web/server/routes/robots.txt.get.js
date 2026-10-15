/**
 * robots.txt。
 *
 * <p>纯静态内容，但放在 server 路由里而不是 `public/robots.txt`：
 * Sitemap 那一行需要站点域名，而域名是运行时才知道的（部署时改 nginx 的
 * `server_name` 即可）。写死在 public/ 里就得在改域名时同步改文件，
 * 是一个必然会被漏掉的步骤。</p>
 */
export default defineEventHandler((event) => {
  const origin = resolveSiteOrigin(event)

  setHeader(event, 'content-type', 'text/plain; charset=utf-8')
  // 内容只随域名变化，交给 nginx 之外的浏览器/CDN 缓存一天足够
  setHeader(event, 'cache-control', 'public, max-age=86400')

  return [
    'User-agent: *',
    'Allow: /',
    '',
    '# 接口只返回 JSON，没有索引价值，屏蔽掉以免浪费百度/Google 的抓取配额',
    'Disallow: /app-api/',
    '',
    `Sitemap: ${origin}/sitemap.xml`,
    ''
  ].join('\n')
})
