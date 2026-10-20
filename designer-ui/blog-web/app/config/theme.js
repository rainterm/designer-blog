/**
 * 主题色与视觉 token（与 admin-web 同步）
 * 视觉方向：运动绿（Athletic Green）
 */
export const colors = {
  primary: '#16A34A',
  primaryHover: '#22C55E',
  primaryPressed: '#15803D',
  primarySuppl: '#22C55E',
  primaryLight: '#86EFAC',
  primarySoft: 'rgba(22, 163, 74, 0.12)',
  accent: '#22C55E',
  body: '#F2F5F3',
  surface: '#FFFFFF',
  border: '#E2E8E4',
  borderStrong: '#D1D9D4',
  text: '#161A17',
  textSecondary: '#55605A',
  textMuted: '#87918B',
  success: '#16A34A',
  codeBg: '#1A2421',
  quoteBg: 'rgba(22, 163, 74, 0.08)',
  quoteText: '#15803D'
}

/** 统一圆角（与后台一致；favicon 由 scripts/syncBrandMark.js 同步） */
export const radius = '6px'

export const layout = {
  maxWidth: '920px',
  articleMaxWidth: '920px',
  navHeight: '64px',
  pagePadding: '24px',
  pagePaddingMobile: '16px'
}

export const fonts = {
  /**
   * 一律用系统字体，不引 webfont。
   *
   * <p>站点内容是中文，中文在任何方案下都由系统字体渲染（PingFang SC / 微软雅黑），
   * webfont 只会额外覆盖拉丁字符与数字。为了这一点字形差异多下载字体文件、
   * 多一条阻塞渲染的链路，不划算。</p>
   *
   * <p>用 `-apple-system` 而不是写死 `Helvetica`：macOS 上才会拿到 SF，
   * Windows 落到 Segoe UI / 微软雅黑，各自都是本平台最自然的观感。</p>
   */
  sans: "-apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', 'Helvetica Neue', Arial, sans-serif",
  display: "-apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', 'Helvetica Neue', Arial, sans-serif",
  /** 等宽同样用系统栈：SF Mono / Menlo / Consolas 都是各自平台的标准等宽字体 */
  mono: "ui-monospace, SFMono-Regular, 'SF Mono', Menlo, Consolas, 'Liberation Mono', monospace"
}

/**
 * CSS 自定义属性的唯一来源。
 *
 * <p>刻意不用 JS 在客户端挂载前把变量写到 `document.documentElement` 上：
 * 服务端渲染时没有 `document`，而变量如果等到客户端才注入，
 * 首屏（也是爬虫唯一看到的那一帧）就是没有配色的裸样式。</p>
 *
 * <p>因此改为构建期生成静态 CSS：`scripts/syncThemeTokens.js` 用这个函数产出
 * `app/assets/styles/tokens.css`，由 `index.css` 顶部 @import 引入。
 * 服务端与客户端拿到的是同一份样式，既没有 FOUC 也不会有 hydration 差异。</p>
 */
export function themeVars() {
  return {
    '--color-primary': colors.primary,
    '--color-primary-hover': colors.primaryHover,
    '--color-primary-pressed': colors.primaryPressed,
    '--color-primary-suppl': colors.primarySuppl,
    '--color-primary-light': colors.primaryLight,
    '--color-primary-soft': colors.primarySoft,
    '--color-accent': colors.accent,
    '--color-body': colors.body,
    '--color-surface': colors.surface,
    '--color-border': colors.border,
    '--color-border-strong': colors.borderStrong,
    '--color-text': colors.text,
    '--color-text-secondary': colors.textSecondary,
    '--color-text-muted': colors.textMuted,
    '--color-success': colors.success,
    '--color-code-bg': colors.codeBg,
    '--color-quote-bg': colors.quoteBg,
    '--color-quote-text': colors.quoteText,
    '--gradient-brand': `linear-gradient(135deg, ${colors.primary}, ${colors.primaryLight})`,
    '--radius': radius,
    '--radius-sm': radius,
    '--radius-md': radius,
    '--radius-lg': radius,
    '--radius-xl': radius,
    '--radius-full': '999px',
    '--layout-max-width': layout.maxWidth,
    '--layout-article-max-width': layout.articleMaxWidth,
    '--layout-nav-height': layout.navHeight,
    '--layout-page-padding': layout.pagePadding,
    '--font-sans': fonts.sans,
    '--font-display': fonts.display,
    '--font-mono': fonts.mono
  }
}

/** 生成 `tokens.css` 的完整文本（供构建期写文件） */
export function themeCssText() {
  const body = Object.entries(themeVars())
    .map(([name, value]) => `  ${name}: ${value};`)
    .join('\n')
  return [
    '/*',
    ' * 由 scripts/syncThemeTokens.js 依据 app/config/theme.js 生成，请勿手改。',
    ' * 改配色请改 theme.js，然后重新执行 dev / build（dev 下保存即自动重建）。',
    ' */',
    ':root {',
    body,
    '}',
    ''
  ].join('\n')
}

/** @type {import('naive-ui').GlobalThemeOverrides} */
export const themeOverrides = {
  common: {
    primaryColor: colors.primary,
    primaryColorHover: colors.primaryHover,
    primaryColorPressed: colors.primaryPressed,
    primaryColorSuppl: colors.primarySuppl,
    borderRadius: radius,
    borderRadiusSmall: radius,
    borderRadiusMedium: radius,
    borderRadiusLarge: radius,
    fontFamily: fonts.sans,
    textColorBase: colors.text,
    textColor2: colors.textSecondary,
    textColor3: colors.textMuted,
    bodyColor: colors.body,
    cardColor: colors.surface,
    borderColor: colors.border,
    dividerColor: colors.border
  },
  Button: {
    borderRadiusMedium: radius,
    heightMedium: '40px',
    fontWeight: '500'
  },
  Input: {
    borderRadius: radius,
    heightMedium: '42px'
  },
  Pagination: {
    itemBorderRadius: radius
  },
  Empty: {
    iconColor: 'rgba(22, 163, 74, 0.35)',
    textColor: colors.textMuted
  }
}

/**
 * 配色派生。
 *
 * <p>上面的 `colors` / `tokens.css` 是**构建期**产物、只含默认色；而站点主题色由后台
 * 「博客 → 站点设置」下发、运行时可改。所以这里按与 admin-web `buildColors` 一致的
 * 算法从主色派生整套品牌色，供运行时覆盖使用。</p>
 */

/** 主色归一化：3 位补全、非法回默认，始终返回大写 #RRGGBB */
export function normalizePrimaryColor(input) {
  if (!input || typeof input !== 'string') return colors.primary
  let hex = input.trim()
  if (!hex.startsWith('#')) hex = `#${hex}`
  if (/^#[0-9a-fA-F]{3}$/.test(hex)) {
    hex = `#${hex[1]}${hex[1]}${hex[2]}${hex[2]}${hex[3]}${hex[3]}`
  }
  return /^#[0-9a-fA-F]{6}$/.test(hex) ? hex.toUpperCase() : colors.primary
}

function hexToRgb(hex) {
  const n = parseInt(normalizePrimaryColor(hex).slice(1), 16)
  return { r: (n >> 16) & 255, g: (n >> 8) & 255, b: n & 255 }
}

function rgbToHex({ r, g, b }) {
  const clamp = (v) => Math.max(0, Math.min(255, Math.round(v)))
  return `#${[clamp(r), clamp(g), clamp(b)]
    .map((v) => v.toString(16).padStart(2, '0'))
    .join('')
    .toUpperCase()}`
}

/** 把颜色按比例混入目标色 */
function mixHex(hex, targetHex, amount) {
  const a = hexToRgb(hex)
  const b = hexToRgb(targetHex)
  const t = Math.max(0, Math.min(1, amount))
  return rgbToHex({
    r: a.r + (b.r - a.r) * t,
    g: a.g + (b.g - a.g) * t,
    b: a.b + (b.b - a.b) * t
  })
}

/** 主色 + 透明度 → rgba(...) */
export function withAlpha(hex, alpha) {
  const { r, g, b } = hexToRgb(hex)
  return `rgba(${r}, ${g}, ${b}, ${alpha})`
}

/**
 * 由主色派生整套品牌色。
 *
 * <p>与主色无关的 body / surface / text 等沿用内置色板；派生比例与 admin-web
 * 保持一致，两个前端的主题色观感才统一。</p>
 */
export function buildColors(primary = colors.primary) {
  const p = normalizePrimaryColor(primary)
  return {
    ...colors,
    primary: p,
    primaryHover: mixHex(p, '#FFFFFF', 0.18),
    primaryPressed: mixHex(p, '#000000', 0.14),
    primarySuppl: mixHex(p, '#FFFFFF', 0.22),
    primaryLight: mixHex(p, '#FFFFFF', 0.55),
    primarySoft: withAlpha(p, 0.12),
    accent: mixHex(p, '#FFFFFF', 0.18),
    success: p,
    quoteBg: withAlpha(p, 0.08),
    quoteText: mixHex(p, '#000000', 0.14)
  }
}

/**
 * 主题色对应的 CSS 变量（仅颜色）。
 *
 * <p>刻意不含 `--layout-page-padding` 之类的 token：它们由 tokens.css 的响应式
 * @media 维护，若被内联样式覆盖，移动端断点就失效了。</p>
 */
export function themeColorVars(primary) {
  const c = buildColors(primary)
  return {
    '--color-primary': c.primary,
    '--color-primary-hover': c.primaryHover,
    '--color-primary-pressed': c.primaryPressed,
    '--color-primary-suppl': c.primarySuppl,
    '--color-primary-light': c.primaryLight,
    '--color-primary-soft': c.primarySoft,
    '--color-accent': c.accent,
    '--color-success': c.success,
    '--color-quote-bg': c.quoteBg,
    '--color-quote-text': c.quoteText,
    '--gradient-brand': `linear-gradient(135deg, ${c.primary}, ${c.primaryLight})`
  }
}

/** 由主色构建 Naive UI 主题覆盖（结构与圆角沿用内置，色值按主色派生） */
export function buildThemeOverrides(primary) {
  const c = buildColors(primary)
  return {
    ...themeOverrides,
    common: {
      ...themeOverrides.common,
      primaryColor: c.primary,
      primaryColorHover: c.primaryHover,
      primaryColorPressed: c.primaryPressed,
      primaryColorSuppl: c.primarySuppl,
      textColorBase: c.text,
      textColor2: c.textSecondary,
      textColor3: c.textMuted,
      bodyColor: c.body,
      cardColor: c.surface,
      borderColor: c.border,
      dividerColor: c.border
    },
    Empty: {
      iconColor: withAlpha(c.primary, 0.35),
      textColor: c.textMuted
    }
  }
}

export default {
  colors,
  radius,
  layout,
  fonts,
  themeVars,
  themeCssText,
  themeOverrides,
  normalizePrimaryColor,
  buildColors,
  themeColorVars,
  buildThemeOverrides
}
