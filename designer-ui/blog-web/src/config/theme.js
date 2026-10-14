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

/** 统一圆角（与后台一致；favicon / logo 由 Vite 插件同步） */
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

export function applyThemeCssVars(target = document.documentElement) {
  const map = {
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
  Object.entries(map).forEach(([key, value]) => {
    target.style.setProperty(key, value)
  })
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
  themeOverrides,
  applyThemeCssVars
}
