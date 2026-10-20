import { DARK_QUERY, useColorMode } from '@/composables/useColorMode'

/**
 * 让运行时的主题状态跟上「首帧已经画出来的那个主题」，并在
 * 「跟随系统」时实时响应系统切换。
 *
 * <p>两件事都不能提前做：</p>
 *
 * <ul>
 *   <li>**同步状态**必须等 `app:mounted`（水合结束）。首帧的 `<html data-theme>`
 *       是内联脚本按 Cookie / 媒体查询写的，服务端渲染 Naive UI 时用的是结果
 *       Cookie；两者在首次访问、或系统偏好变过时会不一致，若在水合前改状态，
 *       客户端首帧就和服务端 HTML 对不上（hydration mismatch）。</li>
 *   <li>**监听系统切换**必须由本插件负责，而不是塞进组合函数 —— 组合函数会被
 *       多个组件各调一次，监听器会重复注册；这里只注册一次，且只在客户端。</li>
 * </ul>
 *
 * <p>偏好被用户显式设成 light / dark 后，系统怎么切都不再影响页面 —— 这正是
 * 「手动指定」与「跟随系统」的区别。</p>
 */
export default defineNuxtPlugin((nuxtApp) => {
  const { preference, isDark, applyResolved } = useColorMode()

  const query = window.matchMedia(DARK_QUERY)

  /** 系统偏好变了：只有还在「跟随系统」时才跟着变 */
  function onSystemChange(event) {
    if (preference.value !== 'system') return
    applyResolved(event.matches)
  }

  query.addEventListener('change', onSystemChange)

  nuxtApp.hook('app:mounted', () => {
    // 以内联脚本已经写好的 DOM 为准：它读的是最新的 Cookie / 媒体查询，
    // 比 payload 里的服务端状态更可信
    const dom = document.documentElement.getAttribute('data-theme')
    applyResolved(dom ? dom === 'dark' : isDark.value)
  })

  // Nuxt 没有 app:unmounted 钩子，用 Vue 的应用级卸载回调收尾
  nuxtApp.vueApp.onUnmount?.(() => {
    query.removeEventListener('change', onSystemChange)
  })
})
