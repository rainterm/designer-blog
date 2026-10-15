import { createDiscreteApi } from 'naive-ui'

/**
 * 脱离组件树的全局消息实例，供请求层等非组件场景使用。
 *
 * <p>SSR 下必须惰性创建：`createDiscreteApi` 会新建一个应用并把挂载点塞进 DOM，
 * 而服务端没有 `document` —— 模块加载期就调用会让整个 SSR 进程报错。</p>
 */
let discrete = null

function getDiscrete() {
  if (!import.meta.client) return null
  if (!discrete) {
    discrete = createDiscreteApi(['message', 'dialog', 'notification'])
  }
  return discrete
}

/**
 * 错误提示。服务端渲染期间静默丢弃 —— 提示是给浏览器里的访客看的。
 *
 * <p>刻意**不叫** `showError`：Nuxt 内置同名函数（跳转到错误页），
 * 而 `utils/` 下的导出会被自动导入、从而覆盖内置的那个。
 * 若误用该名字，「分类不存在 -> 跳 404」这类调用会打到 toast 上，
 * 把错误对象塞给提示、错误页根本不出现。</p>
 */
export function notifyError(text) {
  getDiscrete()?.message.error(text)
}

/** 成功提示 */
export function notifySuccess(text) {
  getDiscrete()?.message.success(text)
}
