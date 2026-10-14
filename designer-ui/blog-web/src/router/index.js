import { createRouter, createWebHistory } from 'vue-router'
import { watch } from 'vue'
import BlogLayout from '@/layout/BlogLayout.vue'
import { applySiteSeo, site } from '@/composables/useSiteSetting'
import { reportVisit } from '@/utils/visitTracker'
import { appTitle } from '@/settings'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: BlogLayout,
      children: [
        {
          path: '',
          name: 'blog',
          component: () => import('@/views/Blog.vue'),
          meta: { title: '文章' }
        },
        {
          path: 'post/:slug',
          name: 'post',
          component: () => import('@/views/Post.vue'),
          meta: { title: '文章详情' }
        },
        {
          path: 'category/:slug',
          name: 'category',
          component: () => import('@/views/Blog.vue'),
          meta: { title: '分类', filterType: 'category' }
        },
        {
          path: 'tag/:slug',
          name: 'tag',
          component: () => import('@/views/Blog.vue'),
          meta: { title: '标签', filterType: 'tag' }
        }
      ]
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/'
    }
  ],
  scrollBehavior() {
    return { top: 0 }
  }
})

/** 路由级 SEO：标题 `页面名 · 站点名`，keywords/description 用站点默认（文章页会覆盖） */
function applyRouteSeo(to) {
  const name = site.siteName || appTitle
  const page = to?.meta?.title
  applySiteSeo(page ? `${page} · ${name}` : name)
}

/**
 * 路由切换时上报访问埋点。
 *
 * <p>在 afterEach 里 fire-and-forget，不 await、不阻塞渲染；
 * 上报失败也不会提示访客（见 {@link reportVisit}）。</p>
 */
function trackVisit(to) {
  // 重定向到首页的兜底路由没有 meta.title，仍按实际路径统计
  reportVisit(to.fullPath)
}

router.afterEach((to) => {
  applyRouteSeo(to)
  trackVisit(to)
})

// 站点设置异步到达后，回填当前页标题与 SEO
watch(
  () => [site.siteName, site.seoKeywords, site.seoDescription, site.siteSubtitle],
  () => applyRouteSeo(router.currentRoute.value)
)

export default router
