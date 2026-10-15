/**
 * Nitro 侧的博客公开接口调用。
 *
 * <p>与 `app/api/request.js` 是同一件事的「服务端版本」，但刻意不共用代码：
 * 那边要处理浏览器侧的相对路径、全局提示、业务码分支；这里只服务于
 * `server/routes/` 下的机器可读端点（robots / sitemap），保持最小实现。</p>
 */

/** 兜底地址：与 nuxt.config 的 runtimeConfig.apiInternalBase 默认值一致 */
const DEFAULT_BASE = 'http://localhost:8080'

/**
 * 调一次公开接口并返回 `data`。
 *
 * @param {import('h3').H3Event} event 用于读取 runtimeConfig
 * @param {string} path 形如 `/app-api/blog/public/posts`
 * @param {object} [query] 查询参数
 */
export async function fetchPublicBlog(event, path, query) {
  const base = String(useRuntimeConfig(event).apiInternalBase || DEFAULT_BASE).replace(/\/+$/, '')

  const res = await $fetch(path, { baseURL: base, query })

  // 后端把所有业务异常也返回 HTTP 200（见 GlobalExceptionFilter），
  // 所以失败判定不能只看状态码，必须检查响应体里的 code。
  if (res && typeof res === 'object' && res.code !== undefined && res.code !== 200) {
    throw new Error(res.msg || `接口返回失败：${path}`)
  }
  return res?.data
}

/**
 * 站点对外访问地址（不带尾部斜杠）。
 *
 * <p>优先用 `NUXT_PUBLIC_SITE_URL`，没配就按当前请求推导 ——
 * nginx 会把 `Host` 与 `X-Forwarded-Proto` 透传过来，所以推导结果是
 * 访客/爬虫真正在用的那个域名（`http://www.rainterm.cn`）。
 * 推导的兜底意义在于：改域名只改 nginx 的 `server_name`，sitemap 会自动跟上。</p>
 */
export function resolveSiteOrigin(event) {
  const configured = String(useRuntimeConfig(event).public.siteUrl || '').trim()
  if (configured) return configured.replace(/\/+$/, '')

  const url = getRequestURL(event)
  return `${url.protocol}//${url.host}`.replace(/\/+$/, '')
}
