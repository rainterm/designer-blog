/**
 * 展示层的小工具。
 *
 * <p>这里**没有日期格式化**：接口返回的时间已经由后端按
 * {@code DateTimePattern.DATE_TIME}（yyyy-MM-dd HH:mm:ss）转成字符串，
 * 前端原样展示即可。在前端再切一刀会带来两个问题：</p>
 * <ul>
 *   <li>同一个时间在列表、详情、后台各格式化一次，格式迟早不一致；</li>
 *   <li>「yyyy-MM-dd HH:mm:ss」是空格分隔的，交给 `new Date()` 在旧版 Safari
 *       会得到 Invalid Date —— 想格式化就得自己解析，等于把时间语义搬到前端。</li>
 * </ul>
 */

/**
 * 阅读量展示
 * @param {number|null|undefined} count
 */
export function formatViews(count) {
  const n = Number(count) || 0
  if (n >= 1000) {
    const k = n / 1000
    return `${k % 1 === 0 ? k.toFixed(0) : k.toFixed(1)}k`
  }
  return String(n)
}
