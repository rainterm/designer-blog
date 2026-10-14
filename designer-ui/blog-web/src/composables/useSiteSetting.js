import { reactive } from 'vue'
import { fetchSiteSetting } from '@/api/blog'
import { brand } from '@/config/site'
import { brandMarkSvg } from '@/config/brandMark'
import { applySeoMeta } from '@/utils/seo'

/**
 * 站点设置（后台可配）：站点名称 / 副标题 / Logo / 页脚版权 / 备案号 / SEO 默认值 / 评论总开关。
 *
 * - 默认值取自 src/config/site.js，后端返回的非空项覆盖之；
 * - 模块级单例，多个组件共享同一份配置；
 * - 接口异常时静默降级为默认值，不阻塞渲染。
 */
export const site = reactive({
  siteName: brand.name,
  siteSubtitle: '',
  logoUrl: '',
  logoRadius: 6,
  copyright: brand.copyright,
  icp: '',
  icpUrl: '',
  seoKeywords: '',
  seoDescription: '',
  /** 全局评论总开关：1 开启，0 关闭（与单篇的 allowComment 是「与」关系） */
  commentEnabled: 1
})

let pending = null

export function useSiteSetting() {
  return site
}

/** SEO 描述兜底：默认描述 → 副标题 → 空 */
export function siteSeoDescription() {
  return site.seoDescription || site.siteSubtitle || ''
}

/** 拉取站点设置（并发调用复用同一请求） */
export function loadSiteSetting() {
  if (!pending) {
    pending = fetchSiteSetting()
      .then((res) => {
        applySite(res.data || {})
        return site
      })
      .catch(() => site)
      .finally(() => {
        pending = null
      })
  }
  return pending
}

/** 合并后端配置：空值保留本地默认 */
export function applySite(data = {}) {
  if (data.siteName) site.siteName = data.siteName
  if (data.siteSubtitle) site.siteSubtitle = data.siteSubtitle
  if (data.logoUrl) site.logoUrl = data.logoUrl
  // 0 是合法圆角（直角），故用 null 判断而非真值
  if (data.logoRadius != null) site.logoRadius = data.logoRadius
  if (data.copyright) site.copyright = data.copyright
  if (data.icp) site.icp = data.icp
  if (data.icpUrl) site.icpUrl = data.icpUrl
  if (data.seoKeywords) site.seoKeywords = data.seoKeywords
  if (data.seoDescription) site.seoDescription = data.seoDescription
  // 0 是合法值（关闭评论），故用 null 判断而非真值
  if (data.commentEnabled != null) site.commentEnabled = data.commentEnabled
  applyFavicon()
}

/** 浏览器标签图标跟随站点 Logo（应用圆角）；未配置时用内置 favicon.svg */

/** favicon 画布尺寸 */
const FAVICON_SIZE = 64

/** 顶栏 Logo 展示尺寸：用于把 px 圆角等比换算到 favicon 画布 */
const LOGO_DISPLAY_SIZE = 28

/** favicon 生成序号，避免异步竞态导致旧图覆盖新图 */
let faviconToken = 0

function ensureIconLink() {
  let link = document.querySelector('link[rel="icon"]')
  if (!link) {
    link = document.createElement('link')
    link.rel = 'icon'
    document.head.appendChild(link)
  }
  // 类型交给浏览器嗅探，避免上传 png/jpg 时与 svg 类型冲突
  link.removeAttribute('type')
  return link
}

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

/** 内置 Logo 的 favicon：按圆角设置生成 SVG data URL（必须转义 # 等字符） */
function builtinFaviconDataUrl(radius) {
  const svg = brandMarkSvg({ rx: toCanvasRadius(radius) })
  return `data:image/svg+xml,${encodeURIComponent(svg)}`
}

/** 把图片按圆角裁剪并等比缩放，绘制为 favicon 的 data URL */
async function buildRoundedFavicon(src, radius) {
  const img = await loadImage(src)
  const canvas = document.createElement('canvas')
  canvas.width = FAVICON_SIZE
  canvas.height = FAVICON_SIZE
  const ctx = canvas.getContext('2d')
  if (!ctx) {
    throw new Error('canvas 不可用')
  }

  // 圆角随画布等比放大（与顶栏 Logo 的观感一致），上限为半边长（圆形）
  roundRectPath(ctx, FAVICON_SIZE, toCanvasRadius(radius))
  ctx.clip()

  // 等比缩放并居中，等价于顶栏 Logo 的 object-fit: contain
  const naturalW = img.naturalWidth || img.width
  const naturalH = img.naturalHeight || img.height
  if (!naturalW || !naturalH) {
    throw new Error('图片尺寸无效')
  }
  const scale = Math.min(FAVICON_SIZE / naturalW, FAVICON_SIZE / naturalH)
  const w = naturalW * scale
  const h = naturalH * scale
  ctx.drawImage(img, (FAVICON_SIZE - w) / 2, (FAVICON_SIZE - h) / 2, w, h)

  return canvas.toDataURL('image/png')
}

function applyFavicon() {
  if (typeof document === 'undefined') return
  const link = ensureIconLink()

  // 未配置 Logo：用内置图形，并按圆角设置重绘（标签图标不受 CSS 影响）
  if (!site.logoUrl) {
    faviconToken++
    link.href = builtinFaviconDataUrl(site.logoRadius)
    return
  }

  const token = ++faviconToken

  // 直角：直接用原图
  if (!(site.logoRadius > 0)) {
    link.href = site.logoUrl
    return
  }

  buildRoundedFavicon(site.logoUrl, site.logoRadius)
    .then((dataUrl) => {
      if (token === faviconToken) link.href = dataUrl
    })
    .catch(() => {
      // 跨域图片会污染 canvas 导致导出失败：退回原始图片，至少保证有图标
      if (token === faviconToken) link.href = site.logoUrl
    })
}

/** 站点级 SEO 默认值（路由切换时使用） */
export function applySiteSeo(title) {
  applySeoMeta({
    title,
    keywords: site.seoKeywords,
    description: siteSeoDescription()
  })
}
