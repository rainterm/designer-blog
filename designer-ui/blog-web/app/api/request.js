import { notifyError } from '@/utils/feedback'

/** 统一业务状态码（与后端 ResultCode 对齐） */
export const CODE = {
  SUCCESS: 200,
  BAD_REQUEST: 400,
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  NOT_FOUND: 404,
  // 429：被限流（应用层 @RateLimit 或 nginx limit_req，两层语义一致）
  TOO_MANY_REQUESTS: 429,
  ERROR: 500
}

/** 请求失败时抛出：带上业务码，供调用方分支处理（如 404 渲染错误页） */
export class ApiError extends Error {
  constructor(message, code) {
    super(message)
    this.name = 'ApiError'
    this.code = code
  }
}

/**
 * 请求基地址。
 *
 * <p>浏览器侧固定用相对路径：同源、由 nginx 反代 `/app-api`，不走这里。
 * 服务端渲染时必须给绝对地址 —— 后端在容器内网（`http://server:8080`）
 * 或本机回环，相对路径在 Node 里无从解析。</p>
 */
function resolveBase() {
  if (!import.meta.server) return ''
  const base = useRuntimeConfig().apiInternalBase
  if (!base) {
    throw new Error('NUXT_API_INTERNAL_BASE 未配置：SSR 期间无法用相对路径访问后端')
  }
  return String(base).replace(/\/+$/, '')
}

/**
 * 统一请求入口。
 *
 * <p>从 axios 换成 `$fetch` 是 SSR 的硬性要求：axios 在服务端没有「页面来源」，
 * 相对路径无法解析；`$fetch` 一套代码两端通用，服务端走内部地址、浏览器走同源相对路径。</p>
 *
 * <p>注意后端把**所有业务异常也返回 HTTP 200**（见 GlobalExceptionFilter），
 * 所以失败判定不能只看状态码，必须检查响应体里的 `code`。</p>
 *
 * @param {string} path 形如 `/app-api/blog/public/posts`
 * @param {object} [options] 透传给 `$fetch`；额外支持 `silent`：
 *   true 表示失败时不弹全局提示（页面级取数用，错误交给错误页/空状态展示）
 */
export async function apiFetch(path, options = {}) {
  const { silent = false, ...rest } = options

  let res
  try {
    res = await $fetch(path, { baseURL: resolveBase(), ...rest })
  } catch (error) {
    // 正常业务路径不会走到这：连不上后端、超时、或网关层非 2xx
    throw fail(error?.data?.msg || error?.message || '网络异常，请稍后重试', error?.status, silent)
  }

  // 非 R 结构：原样返回，别把页面搞崩
  if (res == null || typeof res !== 'object' || res.code === undefined) {
    return res
  }
  if (res.code === CODE.SUCCESS) return res

  throw fail(res.msg || '请求失败', res.code, silent)
}

function fail(message, code, silent) {
  if (!silent) notifyError(message)
  return new ApiError(message, code)
}
