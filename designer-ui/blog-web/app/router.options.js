/**
 * 页面滚动行为。
 *
 * <p>页面真正滚动的是 `layouts/default.vue` 里的 `.blog-scroll` 容器（窗口本身
 * `overflow: hidden`），由该布局 watch `route.fullPath` 自己滚回顶部。
 * 这里固定返回 `{ top: 0 }`，避免 vue-router 的默认
 * 「恢复上次滚动位置」在后退时和布局的滚动逻辑打架。</p>
 */
export default {
  scrollBehavior() {
    return { top: 0 }
  }
}
