/**
 * 纯文本段落换行规范化。
 *
 * <p>从记事本 / Word / 微信里直接贴进编辑器的正文，作者习惯「回车即分段」。
 * CommonMark 却只认空行（`\\n\\n`）为段落边界；单换行在 md-editor 里会变成
 * 同一段里的 {@code <br>}，整篇挤成一堵墙，读起来极不友好。</p>
 *
 * <p>仅在「看起来就是纯文本」时介入：已有空行，或出现标题 / 列表 / 代码围栏 /
 * 引用 / 表格等块级标记时，原样返回，避免破坏正经 Markdown。</p>
 *
 * @param {string} source 原始 Markdown / 纯文本
 * @returns {string}
 */
const MARKDOWN_BLOCK_LINE =
  /^(?:#{1,6}(?:\s|$)|```|~~~|\s*[-*+]\s|\s*\d+\.\s|\s*>|\s*\|)/m

export function normalizeMarkdownParagraphs(source) {
  const md = String(source ?? '').replace(/\r\n/g, '\n')
  if (!md.trim()) return md
  // 已有空行：作者在按 Markdown 写段落，不要改
  if (/\n[ \t]*\n/.test(md)) return md
  // 有块级语法：正经 Markdown，不要改
  if (MARKDOWN_BLOCK_LINE.test(md)) return md
  // 纯文本：单换行提升为段落分隔
  return md.replace(/\n+/g, '\n\n').trim()
}
