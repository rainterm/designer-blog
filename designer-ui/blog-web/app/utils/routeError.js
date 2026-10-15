/**
 * 页面级错误的构造helper。
 *
 * <p>为什么不直接 `createError({ statusCode, message })`：h3 建议长文案放 `message`
 * （`statusMessage` 将来默认会被清洗掉，放那里会突然变成一句空泛的默认提示），
 * 但 `message` 这个位置**框架自己也在用** —— Nuxt 对未匹配路由抛的 404 是
 * `Page not found: /xxxx`，直接展示给访客就是一句英文技术文案。
 *
 * <p>所以这里额外打一个 `data.userFacing` 标记，把「这条 message 是给访客看的」
 * 这件事显式表达出来。`error.vue` 只信任带标记的 `message`，
 * 其它（框架自带的、或未预期的异常）一律回退到按状态码写好的中文兜底文案。
 * `data` 会随错误一起序列化，所以客户端路由跳转触发的 `showError` 也带着标记。</p>
 *
 * <p>另外必须带 `fatal: true`。`nuxt-root.vue` 捕获到错误后，**只在客户端**这样判断：</p>
 *
 * <pre>if (import.meta.server || (isNuxtError(err) &amp;&amp; (err.fatal || err.unhandled)))</pre>
 *
 * <p>服务端渲染时 `import.meta.server` 短路，怎么都能出错误页；但客户端跳转时，
 * 一个既非 fatal 也非 unhandled 的错误只会被当成未处理的异常抛出去 ——
 * URL 变了、内容却还是上一页的，且控制台多一条噪音。这正是「直接打开该 URL 是 404，
 * 从站内点进去却没反应」的成因。</p>
 *
 * @param {number} statusCode HTTP 状态码（404 / 502 ...）
 * @param {string} message 给访客看的中文说明
 */
export function userFacingError(statusCode, message) {
  return { statusCode, message, fatal: true, data: { userFacing: true } }
}
