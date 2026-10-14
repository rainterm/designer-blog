import { createApp } from 'vue'
import { createPinia } from 'pinia'
import naive from 'naive-ui'

import App from './App.vue'
import router from './router'
import permission from './directives/permission'
import { applyThemeCssVars } from './theme'
import { setupSearchActionsAlign } from './utils/searchActionsAlign'
import { appTitle } from './settings'
import DataTable from './components/DataTable'
import SearchBar from './components/SearchBar.vue'
import PageTable from './components/PageTable.vue'
import PaginationBar from './components/PaginationBar.vue'
import Dialog from './components/Dialog.vue'
import DetailDrawer from './components/DetailDrawer.vue'
import './styles/index.css'

applyThemeCssVars()
document.title = appTitle

const app = createApp(App)

// 注意顺序：Pinia 必须先于 Router 安装，路由守卫中会用到 store
app.use(createPinia())
app.use(router)
app.use(naive)
// 覆盖默认表格，注入统一空状态 + 自动高度
app.component('NDataTable', DataTable)
app.component('SearchBar', SearchBar)
app.component('PageTable', PageTable)
app.component('PaginationBar', PaginationBar)
app.component('Dialog', Dialog)
app.component('DetailDrawer', DetailDrawer)

app.directive('perm', permission)

app.mount('#app')
setupSearchActionsAlign()

// 首次导航（守卫内会拉取用户信息/菜单）完成后移除首屏动画；
// 此期间组件层 PageLoading 已接管遮罩，两者交叉淡入淡出不留白屏
router.isReady().finally(() => {
  window.__removeBootLoading && window.__removeBootLoading()
})
