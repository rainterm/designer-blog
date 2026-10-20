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
  /** 行内代码文字色：亮底上要压深，深底上要提亮，不能共用一个值 */
  codeInline: '#15803D',
  quoteBg: 'rgba(22, 163, 74, 0.08)',
  quoteText: '#15803D',
  /** 次级面（评论空状态、待审评论面板等），与 surface 有轻微层次差 */
  surfaceAlt: '#F8F8F8',
  /** 半透明填充（如「回复」小标签），不随 surface 变化 */
  fillSubtle: 'rgba(0, 0, 0, 0.06)',
  /** 吸顶导航：半透明 + 毛玻璃，滚动时透出下方内容 */
  headerBg: 'rgba(255, 255, 255, 0.72)',
  headerBorder: 'rgba(0, 0, 0, 0.06)',
  /** 滚动条 */
  scrollbar: 'rgba(0, 0, 0, 0.22)',
  scrollbarHover: 'rgba(0, 0, 0, 0.35)',
  /** 卡片悬停阴影 */
  shadowCard: '0 8px 20px -10px rgba(0, 0, 0, 0.1)',
  /** 卡片悬停位移（见 paletteVars 的说明） */
  cardHoverLift: '-1px',
  /** 长文阅读色：与次级文字同色，亮色模式下与之前完全一致 */
  reading: '#55605A',
  /** 顶栏搜索框底色 */
  searchBg: '#F2F5F3',
  danger: '#D03050'
}

/**
 * 夜间模式色板。
 *
 * <p>方向是「深色中性面 + 发丝线分层」，而不是给亮色刷一层绿：</p>
 * <ul>
 *   <li>底色接近纯黑（#0B0B0B），内容面只比它亮一点（#161616）——层次靠明度差，
 *       不靠彩色边框，这也是一线移动端深色界面（微信 / X / GitHub Dark）的共同做法；</li>
 *   <li>分隔用白色低透明度发丝线（8% 左右），在任何底色上都自动协调，
 *       写死成某个灰值反而会在不同面上显出「边框感」；</li>
 *   <li>文字用中性白（#EDEDED）而不是纯白：纯白在纯黑上对比过强、久看发涩；</li>
 *   <li>品牌绿照旧作为唯一的彩色出口（链接 / 分类 / 标签），其余一律去饱和。</li>
 * </ul>
 *
 * <p>键与 {@link colors} 一一对应 —— 组件只消费 token，切换模式时不需要改任何结构。</p>
 */
export const darkColors = {
  primary: '#22C55E',
  primaryHover: '#4ADE80',
  primaryPressed: '#16A34A',
  primarySuppl: '#4ADE80',
  primaryLight: '#86EFAC',
  primarySoft: 'rgba(34, 197, 94, 0.14)',
  accent: '#4ADE80',
  /** 画布：接近纯黑，但留 4 个色阶避免 OLED 上的「黑洞感」 */
  body: '#0B0B0B',
  /** 内容面：卡片 / 面板，靠 11 个色阶的明度差与画布分层 */
  surface: '#161616',
  /** 次级面：待审评论面板、空状态这类「面中面」 */
  surfaceAlt: '#1D1D1D',
  /** 发丝线：白色低透明度，叠在任何底色上都不突兀 */
  border: 'rgba(255, 255, 255, 0.08)',
  borderStrong: 'rgba(255, 255, 255, 0.16)',
  text: '#EDEDED',
  textSecondary: '#A3A3A3',
  textMuted: '#7A7A7A',
  success: '#22C55E',
  /** 代码块比画布亮一档：在近黑底上仍然能看出是一个独立区块 */
  codeBg: '#1A1A1A',
  codeInline: '#86EFAC',
  quoteBg: 'rgba(34, 197, 94, 0.1)',
  quoteText: '#86EFAC',
  fillSubtle: 'rgba(255, 255, 255, 0.07)',
  /** 顶栏：几乎与画布同色，不做「亮一块」的浮层，滚动时只留一条发丝线 */
  headerBg: 'rgba(11, 11, 11, 0.9)',
  headerBorder: 'rgba(255, 255, 255, 0.07)',
  scrollbar: 'rgba(255, 255, 255, 0.16)',
  scrollbarHover: 'rgba(255, 255, 255, 0.28)',
  /** 深色下不用投影：黑底上的阴影只会糊成一团，分层交给明度与发丝线 */
  shadowCard: 'none',
  /** 深色下卡片不抬起：位移在近黑画布上读不出「浮起」，只像抖一下 */
  cardHoverLift: '0',
  /** 长文阅读色：在近黑底上保持 12:1 左右的对比，比标题柔和、比次级文字清晰 */
  reading: '#D6D6D6',
  /** 顶栏搜索框底色：比画布提亮一档，否则会和顶栏糊成一片 */
  searchBg: 'rgba(255, 255, 255, 0.07)',
  danger: '#FA5151'
}

/** 统一圆角（与后台一致；favicon 由 scripts/syncBrandMark.js 同步） */
export const radius = '6px'

export const layout = {
  /**
   * 内容列宽度。
   *
   * <p>820px 是「阅读舒适」与「列表不局促」的折中：减去两侧 24px 内边距后正文 772px，
   * 中文 16px 字号一行约 48 字，比 920px 时的 54 字更接近排版学推荐的每行 45 字上下；
   * 同时减去侧边留白后仍够放下 120px 缩略图 + 标题 + 摘要的横向卡片。</p>
   */
  maxWidth: '820px',
  articleMaxWidth: '820px',
  /**
   * 顶栏高度。内部搜索框 34px、图标按钮 30px，上下各留 13px 呼吸空间。
   *
   * <p>直接改这里即可，tokens.css 由 scripts/syncThemeTokens.js 生成（dev 保存自动重建）；
   * 吸顶目录偏移、锚点让位都引用同一个 --layout-nav-height，会自动跟随。</p>
   */
  navHeight: '60px',
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
 * 色板派生变量。
 *
 * <p>不只是颜色：`--card-hover-lift` 这类**随模式变化**的交互参数也放在这里，
 * 因为夜间块（`[data-theme=dark]`）就是由色板生成的 —— 放到别处就得为它再开
 * 一个选择器块，反而更难维护。</p>
 *
 * <p>与 {@link themeVars} 分开，是因为夜间模式只覆盖这一组：圆角 / 字体 / 布局尺寸
 * 在两种模式下完全相同，若一起写进 `:root[data-theme='dark']`，选择器权重会比
 * index.css 里 `@media (max-width: 768px)` 的 `:root` 更高，移动端内边距就再也改不回去了。</p>
 */
function paletteVars(c) {
  return {
    '--color-primary': c.primary,
    '--color-primary-hover': c.primaryHover,
    '--color-primary-pressed': c.primaryPressed,
    '--color-primary-suppl': c.primarySuppl,
    '--color-primary-light': c.primaryLight,
    '--color-primary-soft': c.primarySoft,
    '--color-accent': c.accent,
    '--color-body': c.body,
    '--color-surface': c.surface,
    '--color-surface-alt': c.surfaceAlt,
    '--color-border': c.border,
    '--color-border-strong': c.borderStrong,
    '--color-text': c.text,
    '--color-text-secondary': c.textSecondary,
    '--color-text-muted': c.textMuted,
    '--color-text-reading': c.reading,
    '--color-search-bg': c.searchBg,
    '--color-success': c.success,
    '--color-code-bg': c.codeBg,
    '--color-code-inline': c.codeInline,
    '--color-quote-bg': c.quoteBg,
    '--color-quote-text': c.quoteText,
    '--color-fill-subtle': c.fillSubtle,
    '--color-header-bg': c.headerBg,
    '--color-header-border': c.headerBorder,
    '--color-scrollbar': c.scrollbar,
    '--color-scrollbar-hover': c.scrollbarHover,
    '--shadow-card-hover': c.shadowCard,
    '--card-hover-lift': c.cardHoverLift,
    '--color-danger': c.danger,
    '--gradient-brand': `linear-gradient(135deg, ${c.primary}, ${c.primaryLight})`
  }
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
export function themeVars(mode = 'light') {
  const c = mode === 'dark' ? darkColors : colors
  return {
    ...paletteVars(c),
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
  const cssBody = (vars) =>
    Object.entries(vars)
      .map(([name, value]) => `  ${name}: ${value};`)
      .join('\n')
  return [
    '/*',
    ' * 由 scripts/syncThemeTokens.js 依据 app/config/theme.js 生成，请勿手改。',
    ' * 改配色请改 theme.js，然后重新执行 dev / build（dev 下保存即自动重建）。',
    ' *',
    ' * 夜间模式只覆盖颜色：布局 / 字体 / 圆角在两种模式下相同，',
    ' * 一起写进 [data-theme=dark] 会压过 index.css 里的移动端媒体查询。',
    ' */',
    ':root {',
    cssBody(themeVars('light')),
    '}',
    '',
    ':root[data-theme="dark"] {',
    cssBody(paletteVars(darkColors)),
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
 * <p>与主色无关的 body / surface / text 等沿用对应模式的色板；派生比例与 admin-web
 * 保持一致，两个前端的主题色观感才统一。夜间模式下主色保持不变（站长选的就是它），
 * 但透明/混色比例略作调整，避免在深色底上发灰。</p>
 */
export function buildColors(primary, mode = 'light') {
  const dark = mode === 'dark'
  const palette = dark ? darkColors : colors
  const p = normalizePrimaryColor(primary || palette.primary)
  return {
    ...palette,
    primary: p,
    primaryHover: mixHex(p, '#FFFFFF', dark ? 0.2 : 0.18),
    primaryPressed: mixHex(p, '#000000', dark ? 0.18 : 0.14),
    primarySuppl: mixHex(p, '#FFFFFF', 0.22),
    primaryLight: mixHex(p, '#FFFFFF', dark ? 0.4 : 0.55),
    primarySoft: withAlpha(p, dark ? 0.18 : 0.12),
    accent: mixHex(p, '#FFFFFF', 0.18),
    success: p,
    quoteBg: withAlpha(p, dark ? 0.14 : 0.08),
    quoteText: dark ? mixHex(p, '#FFFFFF', 0.35) : mixHex(p, '#000000', 0.14)
  }
}

/**
 * 主题色对应的 CSS 变量（仅颜色）。
 *
 * <p>刻意不含 `--layout-page-padding` 之类的 token：它们由 tokens.css 的响应式
 * @media 维护，若被内联样式覆盖，移动端断点就失效了。</p>
 */
export function themeColorVars(primary, mode = 'light') {
  const c = buildColors(primary, mode)
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

/** 由主色构建 Naive UI 主题覆盖（结构与圆角沿用内置，色值按主色与模式派生） */
export function buildThemeOverrides(primary, mode = 'light') {
  const c = buildColors(primary, mode)
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
      popoverColor: c.surface,
      modalColor: c.surface,
      borderColor: c.border,
      dividerColor: c.border
    },
    Empty: {
      iconColor: withAlpha(c.primary, 0.35),
      textColor: c.textMuted
    }
  }
}

/** 夜间模式的默认 Naive UI 主题覆盖（站点未自定义主题色时使用） */
export const darkThemeOverrides = buildThemeOverrides(darkColors.primary, 'dark')

export default {
  colors,
  darkColors,
  radius,
  layout,
  fonts,
  themeVars,
  themeCssText,
  themeOverrides,
  darkThemeOverrides,
  normalizePrimaryColor,
  buildColors,
  themeColorVars,
  buildThemeOverrides
}
