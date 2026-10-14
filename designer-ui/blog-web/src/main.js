import { createApp } from 'vue'
import naive from 'naive-ui'

import App from './App.vue'
import router from './router'
import { applyThemeCssVars } from '@/config/theme'
import { loadSiteSetting } from '@/composables/useSiteSetting'
import { appTitle } from './settings'
import './styles/index.css'

applyThemeCssVars()
document.title = appTitle

const app = createApp(App)
app.use(router)
app.use(naive)

/** 站点设置等待上限：超时先渲染，配置到达后由响应式回填 */
const SITE_READY_TIMEOUT = 1200

Promise.race([
  loadSiteSetting(),
  new Promise((resolve) => setTimeout(resolve, SITE_READY_TIMEOUT))
]).finally(() => {
  app.mount('#app')
  router.isReady().finally(() => {
    window.__removeBootLoading && window.__removeBootLoading()
  })
})
