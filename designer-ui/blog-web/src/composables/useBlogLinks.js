import { ref } from 'vue'
import { fetchLinks } from '@/api/blog'

/**
 * 站点链接（社交/外链）全局共享数据。
 * 顶栏与页脚同时挂载，模块级缓存保证只请求一次；失败则静默降级为空。
 */
const links = ref([])
let loaded = false
let pending = null

export function useBlogLinks() {
  function load() {
    if (loaded) {
      return Promise.resolve(links.value)
    }
    if (!pending) {
      pending = fetchLinks()
        .then((res) => {
          links.value = res.data || []
          loaded = true
          return links.value
        })
        .catch(() => {
          links.value = []
          return links.value
        })
        .finally(() => {
          pending = null
        })
    }
    return pending
  }

  return { links, load }
}
