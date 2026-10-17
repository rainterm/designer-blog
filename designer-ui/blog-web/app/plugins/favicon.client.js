import { useFaviconOverride } from '@/composables/useFavicon'
import { useSiteSetting } from '@/composables/useSiteSetting'
import { brandMarkSvg } from '@/config/brandMark'
import { normalizePrimaryColor } from '@/config/theme'

/**
 * 浏览器标签图标跟随站点 Logo（应用圆角）；未配置时用构建期生成的内置图标。
 *
 * <p>纯客户端行为，所以放在 `.client` 插件里：要读图片、要画 canvas，
 * 而且 favicon 本来就不参与 SEO（爬虫不看它）。</p>
 *
 * <p>这里**只改 `useFaviconOverride` 的状态**，由 `useHead`（见 app.vue / error.vue）
 * 去更新 `<link rel="icon">`。直接改 DOM 会和 unhead 抢同一个标签，导致 head 里
 * 出现两个图标链接，浏览器时而挑上传的、时而挑默认的（刷新一次换一个）。</p>
 */

/** favicon 画布尺寸 */
const FAVICON_SIZE = 64

/** 顶栏 Logo 展示尺寸：用于把 px 圆角等比换算到 favicon 画布 */
const LOGO_DISPLAY_SIZE = 28

/** 生成序号，避免异步竞态导致旧图覆盖新图 */
let token = 0

function loadImage(src) {
  return new Promise((resolve, reject) => {
    const img = new Image()
    // 允许 canvas 绘制（跨域未开 CORS 时会加载失败，由调用方回退）
    img.crossOrigin = 'anonymous'
    img.onload = () => resolve(img)
    img.onerror = () => reject(new Error('图片加载失败'))
    img.src = src
  })
}

function roundRectPath(ctx, size, radius) {
  const r = Math.max(0, Math.min(radius, size / 2))
  ctx.beginPath()
  ctx.moveTo(r, 0)
  ctx.arcTo(size, 0, size, size, r)
  ctx.arcTo(size, size, 0, size, r)
  ctx.arcTo(0, size, 0, 0, r)
  ctx.arcTo(0, 0, size, 0, r)
  ctx.closePath()
}

/** 圆角换算：设置值基于顶栏 Logo 展示尺寸（28px），换算到 favicon 画布（64px） */
function toCanvasRadius(radius) {
  const scaled = ((Number(radius) || 0) / LOGO_DISPLAY_SIZE) * FAVICON_SIZE
  return Math.max(0, Math.min(scaled, FAVICON_SIZE / 2))
}

/** 内置 Logo 的 favicon：按圆角 / 主题色生成 SVG data URL（必须转义 # 等字符） */
function builtinFaviconDataUrl(radius, fill) {
  return `data:image/svg+xml,${encodeURIComponent(brandMarkSvg({ rx: toCanvasRadius(radius), fill }))}`
}

/** 把图片按圆角裁剪并等比缩放，绘制为 favicon 的 data URL */
async function buildRoundedFavicon(src, radius) {
  const img = await loadImage(src)
  const canvas = document.createElement('canvas')
  canvas.width = FAVICON_SIZE
  canvas.height = FAVICON_SIZE
  const ctx = canvas.getContext('2d')
  if (!ctx) throw new Error('canvas 不可用')

  // 圆角随画布等比放大（与顶栏 Logo 的观感一致），上限为半边长（圆形）
  roundRectPath(ctx, FAVICON_SIZE, toCanvasRadius(radius))
  ctx.clip()

  // 等比缩放并居中，等价于顶栏 Logo 的 object-fit: contain
  const naturalW = img.naturalWidth || img.width
  const naturalH = img.naturalHeight || img.height
  if (!naturalW || !naturalH) throw new Error('图片尺寸无效')

  const scale = Math.min(FAVICON_SIZE / naturalW, FAVICON_SIZE / naturalH)
  const w = naturalW * scale
  const h = naturalH * scale
  ctx.drawImage(img, (FAVICON_SIZE - w) / 2, (FAVICON_SIZE - h) / 2, w, h)

  return canvas.toDataURL('image/png')
}

function applyFavicon(override, logoUrl, logoRadius, themeColor) {
  const current = ++token

  // 未配置 Logo：用内置图形，并按圆角 / 主题色重绘（标签图标不受 CSS 影响）
  if (!logoUrl) {
    override.value = builtinFaviconDataUrl(logoRadius, themeColor)
    return
  }

  // 直角：直接用原图。清空覆盖值后 useFaviconHref 会退回 site.logoUrl
  if (!(logoRadius > 0)) {
    override.value = ''
    return
  }

  buildRoundedFavicon(logoUrl, logoRadius)
    .then((dataUrl) => {
      if (current === token) override.value = dataUrl
    })
    .catch(() => {
      // 跨域图片会污染 canvas 导致导出失败：退回原始图片，至少保证有图标
      if (current === token) override.value = ''
    })
}

export default defineNuxtPlugin(() => {
  const site = useSiteSetting()
  const override = useFaviconOverride()

  watch(
    () => [site.value.logoUrl, site.value.logoRadius, site.value.themeColor],
    ([logoUrl, logoRadius, themeColor]) =>
      applyFavicon(override, logoUrl, logoRadius, normalizePrimaryColor(themeColor)),
    { immediate: true }
  )
})
