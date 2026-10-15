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
  sans: "'Manrope', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', -apple-system, BlinkMacSystemFont, sans-serif",
  display: "'Manrope', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif",
  mono: "'JetBrains Mono', 'SF Mono', Menlo, Consolas, monospace"
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

export default {
  colors,
  radius,
  layout,
  fonts,
  themeVars,
  themeCssText,
  themeOverrides
}
