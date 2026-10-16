/**
 * 正文标题锚点：生成 id，并把 `#锚点` 解析回对应的标题元素。
 *
 * <p>为什么需要这一层：md-editor-v3 生成标题 id 的默认实现是
 * `({ text }) => text`（见其 MdPreview 的 `mdHeadingId` 默认值），也就是**原样拿标题文字当 id**。
 * 而文章正文里的目录通常是在编辑器 / GitHub 里写好的，锚点遵循 GitHub 的 slug 规则
 * （转小写、去掉标点、空格变连字符）。两边对不上，点击就什么都不会发生 ——
 * 例如标题 `快速开始（本地开发）` 的 id 是 `快速开始（本地开发）`，
 * 而正文里写的是 `[快速开始](#快速开始本地开发)`。</p>
 *
 * <p>所以这里做两件事：
 *   1. {@link headingId} 给标题生成 GitHub 风格的 id，让「正文里写的锚点」直接对上；
 *   2. {@link resolveHeading} 在解析时留一条兜底 —— 精确 id 命中不了，就两边都按
 *      slug 归一化后比较。这样无论作者写的是 slug 锚点还是标题原文锚点，
 *      甚至旧的原文 id，都能跳到正确的位置。</p>
 */

/**
 * 生成 GitHub 风格的标题 id。
 *
 * <p>规则：转小写 → 剔除标点 / 符号（保留字母、数字、中文字、空格与连字符）→
 * 空白折叠成单个连字符。用 Unicode 属性类而不是列举 ASCII 标点，
 * 是为了让全角括号「（）」、间隔号「·」这类中文标点也能被正常剔除 ——
 * 只列 ASCII 的话 `快速开始（本地开发）` 会原样保留括号，和 GitHub 上写好的锚点对不上。</p>
 *
 * <p>刻意保持为**纯函数**：它在 SSR 与客户端水合时都会执行，两边必须得到同一个 id。
 * 任何「按出现顺序去重计数」的实现都会引入状态，在服务端与客户端的调用顺序不一致时
 * 直接造成 hydration 不一致。代价是标题文字完全重复时 id 也相同（跳向第一个），
 * 这在文章里属于罕见情况，换来的是可预测性。</p>
 *
 * <p>入参兼容两种形态：md-editor-v3 的 `mdHeadingId` 回调传的是
 * `{ text, level, index, currentToken, nextToken }` 对象（它的默认实现就是
 * `({ text }) => text`），而本文具内部与 {@link resolveHeading} 用它时传字符串。
 * 不兼容对象形态的话，回调里会拿到 `[object Object]`，所有标题的 id 会变成同一个
 * 由它 slug 化来的值（如 `object-object`），点击锚点全部指向第一个标题。</p>
 *
 * @param {{text?: string}|string} input 标题文本，或 md-editor-v3 回调的上下文对象
 * @returns {string} 可作为 id / URL 片段的值
 */
export function headingId(input) {
  const text = typeof input === 'string' ? input : input?.text
  return String(text ?? '')
    .trim()
    .toLowerCase()
    // 只保留：字母、数字、组合标记、中文等表意文字（\p{L} 已含）、空白、连字符
    .replace(/[^\p{L}\p{N}\p{M}\s-]/gu, '')
    .replace(/\s+/g, '-')
}

/**
 * 解析 `#锚点`，返回正文里对应的标题元素。
 *
 * @param {string} hash 形如 `#快速开始本地开发` 或 `#%E5%BF%AB...`（编码后的中文）
 * @param {ParentNode} root 搜索范围，传正文容器，避免误命中页面其他标题
 * @returns {Element|null}
 */
export function resolveHeading(hash, root) {
  if (!root || !hash) return null

  let raw = String(hash)
  if (raw.startsWith('#')) raw = raw.slice(1)
  if (!raw) return null

  // 锚点里的中文通常是百分号编码的（浏览器地址栏与 markdown 渲染结果都是），先解码
  try {
    raw = decodeURIComponent(raw)
  } catch {
    // 编码不合法就按原样比较，不因为一个坏锚点抛错
  }

  const headings = root.querySelectorAll('h1,h2,h3,h4,h5,h6')

  // 1) 精确命中（本文件生成的 id、以及未被改写过的锚点都走这条）
  for (const h of headings) {
    if (h.id === raw) return h
  }

  // 2) 兜底：两边都按 slug 归一化再比。覆盖「作者用的是标题原文锚点，而 id 已被 slug 化」
  //    以及反过来的情况（id 是原文，锚点是 slug）
  const wanted = headingId(raw)
  if (!wanted) return null
  for (const h of headings) {
    if (h.id && headingId(h.id) === wanted) return h
  }

  return null
}
