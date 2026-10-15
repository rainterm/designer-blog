import { createCaptchaApi } from '@captcha'

/**
 * 滑块验证码的接口客户端（Nuxt 侧传输层适配）。
 *
 * <p>共享模块只声明请求的形状，具体使用哪个 HTTP 客户端由各站自己决定
 * （见 designer-ui/shared/captcha/createCaptchaApi.js）。这里把 Nuxt 的全局 `$fetch` 接上去，
 * 于是整个 blog-web 不必关心端点细节，只需要 import 这个对象。</p>
 *
 * <p>用全局 `$fetch` 而不是自己 import ofetch：两者在客户端行为一致，
 * 但前者在 SSR 期间会带上 Nuxt 的请求上下文。这里虽然只在前端调用（拖拽交互里），
 * 沿用既有写法可以少一处「为什么和别处不一样」的解释成本。</p>
 */
export const captchaApi = createCaptchaApi((path, body) =>
  $fetch(path, { method: 'POST', body })
)
