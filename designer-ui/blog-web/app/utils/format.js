/**
 * 格式化发布时间为 YYYY.MM.DD
 * @param {string|null|undefined} value
 */
export function formatDate(value) {
  if (!value) return ''
  const d = String(value).slice(0, 10).replace(/-/g, '.')
  return d
}

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
