import { setup } from '@css-render/vue3-ssr'

/**
 * 把 naive-ui 的运行时样式收进 SSR 的 HTML。
 *
 * <p>naive-ui 用 css-render 在**运行时**注入样式（没有独立的 css 文件），
 * 服务端渲染出的 HTML 里自然带不上它们。结果是：浏览器先画出服务端 HTML
 * ——分页、输入框、空状态全是没样式的原生控件——要等客户端 JS 跑完才补上，
 * 也就是常说的 FOUC。</p>
 *
 * <p>时机的关键：`collect()` 只能在**应用渲染完成之后、head 序列化之前**调用。
 * 组件是在渲染过程中把样式登记进 css-render 的，早一步只能收到一半。</p>
 *
 * <p>这也正是为什么不能用 `useHead({ style: [{ innerHTML: ... }] })`：
 * unhead 序列化时走的是 `String(tag.textContent ?? tag.innerHTML ?? '')`
 * （见 unhead 的 tagToString），只接受字符串，不接受惰性求值的函数 —— head 里
 * 那条 style 会在渲染中被立刻求值，那时样式还没收集完。
 * 因此这里挂到 unhead 的 `ssr:render` 钩子上，它的入参就是即将被序列化的
 * tags 数组（`ctx.tags`），正是唯一能拿到完整样式的时机。</p>
 */
export default defineNuxtPlugin((nuxtApp) => {
  const { collect } = setup(nuxtApp.vueApp)

  nuxtApp.ssrContext.head.hooks.hook('ssr:render', (ctx) => {
    const css = collect()
    if (!css) return
    ctx.tags.push({ tag: 'style', props: {}, textContent: css, tagPosition: 'head' })
  })
})
