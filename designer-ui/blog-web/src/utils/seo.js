/**
 * SEO meta 标签读写（keywords / description）。
 * 内容为空时移除标签，避免残留上一页的值。
 */

function upsertMeta(name, content) {
  const el = document.head.querySelector(`meta[name="${name}"]`)
  if (!content) {
    el?.remove()
    return
  }
  if (el) {
    el.setAttribute('content', content)
  } else {
    const meta = document.createElement('meta')
    meta.setAttribute('name', name)
    meta.setAttribute('content', content)
    document.head.appendChild(meta)
  }
}

/**
 * @param {{ title?: string, keywords?: string, description?: string }} seo
 */
export function applySeoMeta(seo = {}) {
  if (typeof document === 'undefined') return
  if (seo.title) {
    document.title = seo.title
  }
  upsertMeta('keywords', seo.keywords || '')
  upsertMeta('description', seo.description || '')
}
