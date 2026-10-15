import { reportVisit } from '@/utils/visitTracker'

/**
 * 路由切换时上报访问埋点。
 *
 * <p>只跑在客户端：埋点要的是访客行为，服务端渲染时既不知道访客身份，
 * 也会把爬虫抓取算成访问。</p>
 *
 * <p>首屏需要单独上报一次 —— SSR 直出没有发生客户端导航，
 * `router.afterEach` 不会为初始路由触发。</p>
 */
export default defineNuxtPlugin((nuxtApp) => {
  const router = useRouter()

  router.afterEach((to) => reportVisit(to.fullPath))

  // 首屏（含 SSR 水合）补一次；后端有 60 秒去重窗口，即使和 afterEach 撞车也不会重复计数
  nuxtApp.hook('app:mounted', () => reportVisit(router.currentRoute.value.fullPath))
})
