import naive from 'naive-ui'

/**
 * 全局注册 naive-ui 组件。
 *
 * <p>源码里到处是 `<n-button>` / `<n-input>` 这类 kebab-case 标签，靠全局注册解析。
 * 所以用一个插件在应用启动时 `vueApp.use(naive)`。</p>
 *
 * <p>顺带说明：naive-ui 的 `create()` 默认导出会把**全部**组件注册进来，
 * 代价是 Vite 对它的 tree-shaking 失效。这是刻意的取舍 ——
 * 换成按需引入要改掉所有模板标签，属于另一件事。</p>
 */
export default defineNuxtPlugin((nuxtApp) => {
  nuxtApp.vueApp.use(naive)
})
