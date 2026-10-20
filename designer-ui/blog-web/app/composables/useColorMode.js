/**
 * 日间 / 夜间模式（含「跟随系统」）。
 *
 * <p>偏好是三态：`system` / `light` / `dark`，默认 `system` —— 系统是夜间就自动用夜间，
 * 与主流实现（next-themes、@nuxtjs/color-mode）的语义一致。</p>
 *
 * <p>状态分四层，各管一段：</p>
 *
 * <ol>
 *   <li>**偏好 Cookie**（`blog-theme` = system/light/dark）：用户点过什么。</li>
 *   <li>**结果 Cookie**（`blog-theme-resolved` = light/dark）：系统偏好在服务端读不到，
 *       所以由浏览器把「这次算出来的结果」记下来，服务端据此渲染 Naive UI 与正文预览器，
 *       返回的首屏就是对的。</li>
 *   <li>**内联脚本**（见 nuxt.config.js 的 head）：在首帧绘制前按 Cookie / 媒体查询
 *       写好 `<html data-theme>`。首次访问、或系统偏好在这段时间里变过时，
 *       结果 Cookie 可能过期，靠它兜底，避免闪白。</li>
 *   <li>**useState**：页面内共享的响应式状态，驱动需要 JS 才能换肤的部分
 *       （Naive UI 的 theme、md-editor-v3 的 theme、按钮图标）。</li>
 * </ol>
 *
 * <p>`system` 偏好下的实时跟随（系统切换时页面跟着变）由
 * `plugins/color-mode.client.js` 里的 `matchMedia` 监听负责 —— 那必须等水合完成
 * 才能改状态，否则客户端首帧会和服务端 HTML 不一致。</p>
 */

/** 偏好：system / light / dark。内联脚本里硬编码了同名 Cookie，两处必须一致 */
export const COLOR_MODE_COOKIE = 'blog-theme'

/** 算出来的结果：light / dark。仅用于服务端首屏渲染 */
export const COLOR_MODE_RESOLVED_COOKIE = 'blog-theme-resolved'

/** `useState` 的 key */
export const COLOR_MODE_STATE = 'blog:color-mode'
export const COLOR_MODE_RESOLVED_STATE = 'blog:color-mode-resolved'

/** 系统夜间偏好的媒体查询 */
export const DARK_QUERY = '(prefers-color-scheme: dark)'

const COOKIE_OPTIONS = { path: '/', maxAge: 60 * 60 * 24 * 365, sameSite: 'lax' }

/** 只有明确的 light / dark 才算偏好，其余（空、脏数据、旧版本）一律回到 system */
export function normalizeColorPreference(value) {
  return value === 'light' || value === 'dark' ? value : 'system'
}

/** 系统当前是否偏好夜间（仅浏览器可用；服务端没有这个概念） */
export function systemPrefersDark() {
  if (!import.meta.client || typeof window === 'undefined') return false
  if (typeof window.matchMedia !== 'function') return false
  return window.matchMedia(DARK_QUERY).matches
}

export function useColorMode() {
  const preferenceCookie = useCookie(COLOR_MODE_COOKIE, COOKIE_OPTIONS)
  const resolvedCookie = useCookie(COLOR_MODE_RESOLVED_COOKIE, COOKIE_OPTIONS)

  /** 用户偏好：system / light / dark */
  const preference = useState(COLOR_MODE_STATE, () =>
    normalizeColorPreference(preferenceCookie.value)
  )

  /**
   * 当前实际生效的模式。
   *
   * <p>显式偏好优先：用户选过日间 / 夜间，就不该再参考结果 Cookie（那可能是一次
   * 系统偏好留下的旧值）。只有 `system` 才用结果 Cookie —— 系统偏好在服务端读不到，
   * 那一次的计算结果由浏览器记了回来。两个 Cookie 都缺失（首次访问）时按日间渲染，
   * 随后内联脚本再用媒体查询修正 `<html data-theme>`，Naive UI 则在挂载后补齐。</p>
   */
  const isDark = useState(COLOR_MODE_RESOLVED_STATE, () => {
    if (preference.value !== 'system') return preference.value === 'dark'
    return resolvedCookie.value === 'dark'
  })

  /** 'light' | 'dark'：给组件的 theme 属性用 */
  const mode = computed(() => (isDark.value ? 'dark' : 'light'))

  /**
   * 应用一个具体结果：状态、结果 Cookie、DOM 属性三者一起改。
   * 只传结果而不读媒体查询，是为了让「跟随系统」和「手动指定」共用同一条落地路径。
   */
  function applyResolved(dark) {
    const value = dark === true
    isDark.value = value
    if (!import.meta.client) return

    const target = value ? 'dark' : 'light'
    // 值没变就不重复写：每次挂载都写一遍 Cookie，等于给每个响应都加了 Set-Cookie 噪音
    if (resolvedCookie.value !== target) resolvedCookie.value = target
    // 内联脚本被 CSP 拦掉时属性可能缺失，这里补上（值相同则不动，避免无谓的样式重算）
    if (document.documentElement.getAttribute('data-theme') !== target) {
      document.documentElement.setAttribute('data-theme', target)
    }
  }

  /**
   * 设置偏好。
   * 选 system 时立刻按当前系统偏好结算一次；选 light / dark 时直接生效。
   */
  function setPreference(next) {
    const value = normalizeColorPreference(next)
    preference.value = value
    preferenceCookie.value = value
    applyResolved(value === 'system' ? systemPrefersDark() : value === 'dark')
  }

  return { preference, isDark, mode, setPreference, applyResolved }
}
