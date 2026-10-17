import { useSiteSetting } from '@/composables/useSiteSetting'

/** 未配置站点 Logo 时构建期生成的默认图标（见 scripts/syncBrandMark.js） */
export const DEFAULT_FAVICON = '/favicon.svg'

/**
 * 客户端生成的 favicon 覆盖值（data URL）。
 *
 * <p>SSR 阶段为空：上传的图片要先画到 canvas 上裁圆角才能当图标，只能在浏览器里做。
 * 空值表示「没有客户端覆盖」，此时 {@link useFaviconHref} 退回 Logo 原图 / 默认图标。</p>
 */
export function useFaviconOverride() {
  return useState('blog:favicon-override', () => '')
}

/**
 * 标签页图标地址，必须交给 `useHead` 统一渲染。
 *
 * <p>不要像以前那样在插件里直接改 `<link rel="icon">` 的 href：那个标签由 unhead 渲染，
 * 绕过它改 DOM 会让 unhead 在 hydration 时认不出自己的标签，于是再补一个默认图标 ——
 * head 里同时存在两个 `rel="icon"`，浏览器选哪个并不确定，刷新时就表现为
 * 「标签页图标时而显示上传的 Logo、时而显示默认图标」。</p>
 *
 * <p>SSR 首帧直接用上传的 Logo（还没裁圆角，但至少不会先闪一下默认图标），
 * 客户端再由 `plugins/favicon.client.js` 换成圆角版本。</p>
 */
export function useFaviconHref() {
  const site = useSiteSetting()
  const override = useFaviconOverride()
  return computed(() => override.value || site.value.logoUrl || DEFAULT_FAVICON)
}
