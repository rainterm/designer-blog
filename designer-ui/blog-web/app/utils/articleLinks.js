/**
 * 正文链接的分类工具。
 *
 * <p>文章正文（尤其是从仓库 README 直接搬过来的）里常有大量**仓库内相对路径**链接，
 * 例如 `docs/开发规范.md`、`deploy/README.md`、`LICENSE`、`designer-ui/sso-demo`。
 * 这些是相对「代码仓库根目录」的路径，博客站点上并不存在对应文件，
 * 点击后浏览器会按「相对当前文章 URL」解析，得到一个必然 404 的地址。</p>
 *
 * <p>所以需要把这类链接识别出来单独处理（见 pages/post/[slug].vue 的 onArticleClick）。</p>
 */

/**
 * 「可正常跳转」的 href：带协议的绝对地址（`https:` / `mailto:` …）、
 * 站内根路径（`/post/xxx`）、页内锚点（`#foo`）。
 */
const NOT_REPO_RELATIVE = /^(?:[a-z][a-z0-9+.-]*:|\/|#)/i

/**
 * 判断链接是否为「仓库内相对路径」。
 *
 * <p>判定方式是**排除法**：只要不是协议地址、不是站内根路径、不是页内锚点，
 * 就认为是仓库相对路径。这样 `docs/a.md`、`./a.md`、`../a.md`、裸文件名 `LICENSE`
 * 都能覆盖到，不需要去猜路径长什么样。</p>
 *
 * @param {string} href 原始 href（未解码即可，判定只看前缀）
 * @returns {boolean}
 */
export function isRepoRelativeLink(href) {
  const value = String(href ?? '').trim()
  if (!value) return false
  return !NOT_REPO_RELATIVE.test(value)
}
