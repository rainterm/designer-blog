<template>
  <div class="blog-page fade-in">
    <nav v-if="categories.length" class="cat-nav" aria-label="分类">
      <!--
        分类导航用 NuxtLink 而不是按钮：爬虫需要一个能跟随的 href，
        否则「分类页」在站内没有任何入链，等同于不存在。
      -->
      <NuxtLink to="/" class="cat-nav__item" :class="{ active: !categorySlug && !tagSlug }">
        全部
      </NuxtLink>
      <NuxtLink
        v-for="c in categories"
        :key="c.id"
        :to="categoryLink(c.slug)"
        class="cat-nav__item"
        :class="{ active: categorySlug === c.slug }"
      >
        {{ c.name }}
      </NuxtLink>
    </nav>

    <n-spin :show="pending">
      <div v-if="posts.length" class="post-list">
        <PostCard v-for="p in posts" :key="p.id" :post="p" />
      </div>
      <n-empty v-else-if="!pending" :description="blogPage.emptyText" style="margin-top: 56px" />
    </n-spin>

    <div v-if="total > pageSize" class="pager">
      <PaginationBar
        :page="pageNum"
        :page-size="pageSize"
        :item-count="total"
        :show-size-picker="false"
        :show-quick-jumper="false"
        @update:page="changePage"
      />
    </div>
  </div>
</template>

<script setup>
import PostCard from '@/components/PostCard.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import { fetchCategories, fetchPosts, fetchTags } from '@/api/blog'
import { CODE } from '@/api/request'
import { blogPage } from '@/config/site'
import { useBlogSeo } from '@/composables/useBlogSeo'
import { userFacingError } from '@/utils/routeError'
// 显式导入 Nuxt 内置的 showError（跳错误页），避免与 utils 下的同名导出混淆
import { showError, useNuxtApp } from '#app'

/** setup 期间先抓住实例：watch 回调里再去取就已经脱离上下文了（见下方 watch） */
const nuxtApp = useNuxtApp()

const props = defineProps({
  /** 分类 slug（分类页传入） */
  categorySlug: { type: String, default: '' },
  /** 标签 slug（标签页传入） */
  tagSlug: { type: String, default: '' }
})

const route = useRoute()
const router = useRouter()

const pageSize = blogPage.pageSize || 10

const keyword = computed(() => (typeof route.query.q === 'string' ? route.query.q.trim() : ''))

/** 页码放 URL（`?page=N`）：刷新不丢页，也是第 2 页之后文章的收录入口 */
const pageNum = computed(() => {
  const raw = Number(route.query.page)
  return Number.isFinite(raw) && raw > 0 ? Math.floor(raw) : 1
})

/**
 * 分类 / 标签清单。
 * 固定 key + 服务端取数：SSR 渲染时拿到并写进 payload，客户端水合后不再重复请求。
 */
const { data: categoriesData } = await useAsyncData(
  'blog:categories',
  () => fetchCategories({ silent: true }).then((r) => r.data || []).catch(() => []),
  { default: () => [] }
)
const { data: tagsData } = await useAsyncData(
  'blog:tags',
  () => fetchTags({ silent: true }).then((r) => r.data || []).catch(() => []),
  { default: () => [] }
)

const categories = computed(() => categoriesData.value || [])

/**
 * 列表数据。
 *
 * <p>key 必须是**响应式**的：`useAsyncData` 内部会 watch key，key 一变就换一份
 * 缓存条目并重新执行（见 nuxt 的 asyncData.js）。用固定 key 会在客户端跳转时
 * 命中上一次的缓存，渲染出「上一个筛选条件」的文章。</p>
 *
 * <p>handler 里把异常收敛成结果对象而不是往上抛：首屏渲染必须能区分
 * 「真的没有文章」和「后端说这个分类不存在」——后者要回一个真正的 404。</p>
 */
const { data: listResult, status } = await useAsyncData(
  () => `blog:posts:${props.categorySlug}|${props.tagSlug}|${keyword.value}|${pageNum.value}`,
  async () => {
    try {
      const res = await fetchPosts(
        {
          pageNum: pageNum.value,
          pageSize,
          keyword: keyword.value || undefined,
          categorySlug: props.categorySlug || undefined,
          tagSlug: props.tagSlug || undefined
        },
        { silent: true }
      )
      return { ok: true, data: res.data }
    } catch (error) {
      return { ok: false, code: error.code, message: error.message }
    }
  },
  { default: () => ({ ok: true, data: null }) }
)

const pending = computed(() => status.value === 'pending')
const posts = computed(() => (listResult.value?.ok ? listResult.value.data?.records || [] : []))
const total = computed(() =>
  listResult.value?.ok ? Number(listResult.value.data?.total) || 0 : 0
)

// 分类 / 标签不存在（后端 404）：这不是「暂无文章」，而是页面本身不存在
if (listResult.value && !listResult.value.ok) {
  throw createError(toRouteError(listResult.value))
}

/**
 * 客户端跳转时 props 变化会触发上面那个 key 重新取数，但 setup 不会重跑，
 * 所以还要盯一次结果 —— 否则从 `/category/vue` 点到不存在的分类会显示
 * 「暂无文章」，和直接打开该 URL 得到的 404 表现不一致
 * （URL 变了、内容却还是上一页的，比报错更糟）。
 * 服务端渲染期间 watch 不会触发，首屏那条路径由上面的 throw 兜住。
 */
watch(listResult, (result) => {
  if (!result || result.ok) return
  /**
   * 必须用 `runWithContext` 包一层。
   *
   * <p>watch 回调是异步微任务，执行时已经脱离 Nuxt 上下文；而 `showError`
   * 内部要 `useNuxtApp()` / `useError()` 去读并写入错误状态。上下文缺失时它
   * 会抛错、再被自己的 `catch` 重新抛出，结果是一个未处理的 rejection ——
   * 错误页不会渲染，控制台还多一条噪音。Nuxt 自己的 `_showErrorUnlessCrawler`
   * 也是这么包的。</p>
   */
  nuxtApp.runWithContext(() => showError(toRouteError(result)))
})

/**
 * 业务失败 -> 页面级 HTTP 错误。
 *
 * <p>`userFacingError` 负责把文案放在正确的位置并打上「可展示」标记，
 * 原因见 app/utils/routeError.js。</p>
 */
function toRouteError(result) {
  return result.code === CODE.NOT_FOUND
    ? userFacingError(404, result.message)
    : userFacingError(502, '文章列表加载失败，请稍后重试')
}

/** 当前筛选条件的中文名，用于标题 */
const activeCategory = computed(() => categories.value.find((c) => c.slug === props.categorySlug))
const activeTag = computed(() => (tagsData.value || []).find((t) => t.slug === props.tagSlug))

const seoTitle = computed(() => {
  if (activeCategory.value) return activeCategory.value.name
  if (activeTag.value) return `#${activeTag.value.name}`
  return blogPage.title
})

useBlogSeo({
  pageTitle: computed(() =>
    pageNum.value > 1 ? `${seoTitle.value}（第 ${pageNum.value} 页）` : seoTitle.value
  ),
  description: computed(() => {
    if (activeCategory.value) return `${activeCategory.value.name} 分类下的全部文章。`
    if (activeTag.value) return `带有 #${activeTag.value.name} 标签的全部文章。`
    return ''
  }),
  // 站内搜索结果页不进索引，否则关键词组合会无限膨胀成垃圾索引
  noindex: () => Boolean(keyword.value)
})

function categoryLink(slug) {
  return { path: `/category/${slug}`, query: keyword.value ? { q: keyword.value } : {} }
}

function changePage(page) {
  const query = { ...route.query }
  if (page > 1) query.page = String(page)
  else delete query.page
  router.push({ path: route.path, query })
}
</script>

<style scoped>
.cat-nav {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 16px;
}

.cat-nav__item {
  flex-shrink: 0;
  border: none;
  background: transparent;
  color: var(--color-text-secondary);
  font-family: inherit;
  font-size: 14px;
  font-weight: 500;
  padding: 6px 10px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: color 0.15s, background 0.15s;
}

.cat-nav__item:hover {
  color: var(--color-text);
  background: rgba(22, 163, 74, 0.08);
}

.cat-nav__item.active {
  color: var(--color-primary);
  background: var(--color-primary-soft);
}

.post-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-top: 8px;
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 40px;
  padding-bottom: 8px;
}
.pager :deep(.pagination-wrap) {
  margin-top: 0;
  padding-top: 0;
  justify-content: center;
}
</style>
