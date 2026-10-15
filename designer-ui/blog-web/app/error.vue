<template>
  <AppShell>
    <NuxtLayout>
      <div class="error-page">
        <p class="error-page__code">{{ statusCode }}</p>
        <h1 class="error-page__title">{{ title }}</h1>
        <p class="error-page__desc">{{ description }}</p>
        <n-button type="primary" @click="goHome">返回文章列表</n-button>
      </div>
    </NuxtLayout>
  </AppShell>
</template>

<script setup>
import { useSiteSetting } from '@/composables/useSiteSetting'

/**
 * 错误页。
 *
 * <p>它返回**真实的 HTTP 状态码**（404 / 502），而不是「先 200 返回空壳、
 * 再在浏览器里显示找不到」—— 后者在搜索引擎眼里是典型的 soft 404：
 * 页面明说不存在，状态码却说一切正常，结果是一堆无效 URL 被索引。</p>
 *
 * <p>注意 provider 要自己拉起来：error.vue 与 app.vue 是互斥渲染的，
 * 出错时 app.vue 不参与渲染（见 AppShell.vue 里的说明）。</p>
 */
const props = defineProps({
  error: { type: Object, default: null }
})

const statusCode = computed(() => Number(props.error?.statusCode) || 500)

const title = computed(() => {
  if (statusCode.value === 404) return '页面不存在'
  if (statusCode.value === 502 || statusCode.value === 503) return '服务暂时不可用'
  return '页面出错了'
})

/** 按状态码的兜底说明：没有 「可展示」文案时用它，不展示框架的原始 message */
const DEFAULT_DESCRIPTIONS = {
  404: '这个地址下没有内容，可能是链接过期或文章已被移除。',
  502: '服务端处理这个页面时出了点问题，稍后再试试。',
  503: '服务暂时不可用，稍后再试试。'
}

/**
 * 说明文字。
 *
 * <p>只采用带 `data.userFacing` 标记的 `message`（由 `userFacingError` 打上，
 * 见 app/utils/routeError.js）。框架自带的错误也在用 `message` ——
 * 例如未匹配路由的 404 是 `Page not found: /xxxx`，那是英文技术文案，
 * 直接展示给中文访客既不友好、也没必要暴露。</p>
 */
const description = computed(() => {
  if (props.error?.data?.userFacing && props.error?.message) {
    return props.error.message
  }
  return DEFAULT_DESCRIPTIONS[statusCode.value] || '服务端处理这个页面时出了点问题，稍后再试试。'
})

/**
 * 标题与收录策略。
 *
 * <p>不设的话 `<title>` 会停在上一页 —— 客户端跳转进入错误页时，unhead 还留着
 * 前一个页面的标题，访客看到的是「URL 说不存在、标签页却写着上一篇」。
 * 服务端直接打开时更是完全没设过。</p>
 *
 * <p>`noindex` 是给爬虫的态度：这个 URL 已经没有内容了。虽然 404/502 本身
 * 就已经表达了这一点，但错误页可能被 CDN 或浏览器缓存后以 200 复用，
 * 显式写死更稳妥。</p>
 */
const site = useSiteSetting()
// 与 useBlogSeo 同样的理由：组合函数依赖当前 Nuxt 实例，
// 必须在 setup 阶段取出来，不能塞进 computed 的 getter
const config = useRuntimeConfig()
const siteName = computed(() => site.value.siteName || config.public.appTitle)

useHead({
  title: computed(() => `${title.value} · ${siteName.value}`),
  meta: [{ name: 'robots', content: 'noindex, follow' }]
})

function goHome() {
  clearError({ redirect: '/' })
}
</script>

<style scoped>
.error-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  min-height: 52vh;
  text-align: center;
}

.error-page__code {
  font-family: var(--font-display);
  font-size: 64px;
  font-weight: 700;
  line-height: 1;
  color: var(--color-primary);
  letter-spacing: -0.03em;
}

.error-page__title {
  font-family: var(--font-display);
  font-size: 22px;
  font-weight: 600;
  color: var(--color-text);
}

.error-page__desc {
  max-width: 420px;
  font-size: 14px;
  color: var(--color-text-secondary);
  margin-bottom: 8px;
}
</style>
