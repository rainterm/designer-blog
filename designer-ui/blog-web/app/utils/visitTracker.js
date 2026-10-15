/**
 * 访问埋点上报。
 *
 * <p>刻意**不走** {@link @/api/request}：那一层的失败会弹全局提示，
 * 而埋点是附加能力，访客不该因为「统计没上报成功」看到报错。
 * 这里直接用一个最小 `$fetch`，吞掉所有异常，返回的 Promise **永不 reject**。</p>
 *
 * <p>只上报路径，**IP / UA / 来源 / 设备 / 页面归属分类全部由服务端从请求推导**，
 * 前端无法伪造统计。服务端对「同一访客 + 同一路径」有 60 秒去重窗口，
 * 所以刷新页面、重复上报不会重复计数。</p>
 *
 * @param {string} path 访问路径（含 query 亦可，后端会自行剥离）
 * @returns {Promise<void>} 永不 reject，调用方无需 catch
 */
export function reportVisit(path) {
  // 服务端渲染期间不上报：埋点统计的是访客行为，由浏览器发出才带得上访客身份
  if (!path || !import.meta.client) return Promise.resolve()
  return $fetch('/app-api/blog/public/visit', {
    method: 'POST',
    body: { path },
    timeout: 5000
  })
    .then(() => undefined)
    .catch(() => undefined)
}
