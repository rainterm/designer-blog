/**
 * 评论人匿名标识。
 *
 * 博客没有读者账号，只能用浏览器本地随机密钥识别「哪些评论是我的」：
 * 密钥存在 localStorage，提交评论时一并上传，服务端只存它的 SHA-256。
 *
 * 注意：清除浏览器数据 / 换设备后会拿到新密钥，届时无法再管理旧评论
 * （匿名评论的固有取舍）。
 */
const STORAGE_KEY = 'blog:commenterKey'

/** localStorage 不可用时的内存态兜底密钥 */
let memoryKey = ''

/** 生成 32 位十六进制随机串 */
function randomKey() {
  // 优先用 CSPRNG；非安全上下文（如 http 访问非 localhost）下 crypto.randomUUID 不可用
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID().replace(/-/g, '')
  }
  const bytes = new Uint8Array(16)
  if (typeof crypto !== 'undefined' && typeof crypto.getRandomValues === 'function') {
    crypto.getRandomValues(bytes)
  } else {
    for (let i = 0; i < bytes.length; i++) {
      bytes[i] = Math.floor(Math.random() * 256)
    }
  }
  return Array.from(bytes, (b) => b.toString(16).padStart(2, '0')).join('')
}

/**
 * 读取当前浏览器的评论人标识，没有就生成并持久化。
 * localStorage 不可用（隐私模式等）时返回内存态密钥，当次会话仍可正常评论。
 *
 * <p>服务端渲染期间返回空串：这份标识只属于某个具体浏览器，服务端既没有
 * localStorage 也不该为它生成密钥 —— 真生成了，它会变成所有访客共用的身份
 * （模块级 `memoryKey` 在 Node 进程里是共享的）。调用点都在客户端事件里，
 * 正常流程不会走到这个分支。</p>
 */
export function getCommenterKey() {
  if (!import.meta.client) return ''
  try {
    const cached = localStorage.getItem(STORAGE_KEY)
    if (cached) return cached
    const key = randomKey()
    localStorage.setItem(STORAGE_KEY, key)
    return key
  } catch {
    if (!memoryKey) memoryKey = randomKey()
    return memoryKey
  }
}
