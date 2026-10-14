import axios from 'axios'

/**
 * 访问埋点上报。
 *
 * <p>刻意**不使用** {@code @/api/request}：那个实例的响应拦截器会在任何失败时弹
 * message.error。埋点是附加能力，访客不该因为「统计没上报成功」看到报错。</p>
 *
 * <p>因此这里自建一个最小 axios 实例，超时更短，并且静默吞掉所有异常。
 * 后端也保证该接口永远返回成功（服务端已吞掉内部异常）。</p>
 */
const beacon = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '',
  timeout: 5000
})

/**
 * 上报一次页面访问。
 *
 * @param {string} path 访问路径（含 query 亦可，后端会自行剥离）
 * @returns {Promise<void>} 永不 reject，调用方无需 catch
 */
export function reportVisit(path) {
  if (!path) return Promise.resolve()
  return beacon
    .post('/app-api/blog/public/visit', { path })
    .then(() => undefined)
    .catch(() => undefined)
}
