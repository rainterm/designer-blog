<template>
  <div class="post-page fade-in">
    <n-spin :show="pending">
      <template v-if="post">
        <div class="post-layout">
          <!--
            大纲是纯导航辅助，且 MdCatalog 依赖真实 DOM 测量，SSR 无意义。
            放进 ClientOnly 既避免它在服务端渲染时出错，也让它天然只在挂载后出现
            —— 服务端与客户端首帧都是「没有大纲」，不存在 hydration 不一致。
          -->
          <ClientOnly>
            <aside v-if="showToc" class="post-toc" aria-label="文章大纲">
              <div class="post-toc__inner">
                <p class="post-toc__title">大纲</p>
                <MdCatalog
                  class="post-toc__catalog"
                  :editor-id="catalogEditorId"
                  :scroll-element="scrollEl"
                  :offset-top="tocOffset"
                  :scroll-element-offset-top="tocOffset"
                  :catalog-max-depth="3"
                />
              </div>
            </aside>
          </ClientOnly>

          <div class="post-main">
            <header class="post-header">
              <h1>{{ post.title }}</h1>
              <div class="post-header__meta">
                <span v-if="post.publishedAt">{{ formatDate(post.publishedAt) }}</span>
                <span v-if="post.authorName">{{ post.authorName }}</span>
                <span>{{ formatViews(post.viewCount) }} 阅读</span>
              </div>
              <div v-if="displayTags.length" class="post-header__tags">
                <NuxtLink
                  v-for="t in displayTags"
                  :key="t.slug || t.name"
                  :to="`/tag/${t.slug}`"
                  class="tag-link"
                  :prefetch="false"
                >
                  #{{ t.name }}
                </NuxtLink>
              </div>
            </header>

            <n-image-group>
              <div v-if="coverSrc" class="post-cover">
                <!--
                  object-fit 必须是 contain：封面按原图比例完整展示。
                  用 cover 会为了填满固定高度的容器而裁掉上下两端（图片一旦被判为
                  「横幅」，原图里的顶栏 / 底栏就会被切掉），这里以「看全」优先。
                -->
                <n-image :src="coverSrc" :alt="post.title" object-fit="contain" width="100%" />
              </div>

              <div class="article-body">
                <!--
                  直接渲染 md-editor-v3 预览器：与后台编辑器预览同一套渲染与样式
                  （代码块头部 / 复制按钮 / 折叠、图片点击放大均由组件自带）。

                  SSR 的关键点在这里：MdPreview 的正文是在**渲染函数里**产出的
                  （unhead 之外的另一半收益），所以文章全文会出现在服务端返回的
                  HTML 里，爬虫不需要执行任何 JS 就能读到。

                  onGetCatalog 回传正文标题列表，用来决定左侧大纲是否显示。
                -->
                <MdPreview
                  :id="catalogEditorId"
                  :model-value="contentMd"
                  language="zh-CN"
                  :on-get-catalog="handleCatalog"
                />
              </div>
            </n-image-group>

            <section class="comments">
              <h3>评论</h3>

              <!-- 两级开关都开启才渲染输入区（站点总开关 + 单篇 allowComment） -->
              <template v-if="commentable">
                <CommentForm
                  :post-id="post.id"
                  :reply-to="replyTo"
                  @success="onCommentSuccess"
                  @cancel-reply="replyTo = null"
                />
                <!-- 自己的待审评论：审核通过前公开列表里看不到，这里给作者一个确认和撤回的入口 -->
                <MyCommentPanel :post-id="post.id" :refresh-token="myCommentRefresh" />
              </template>
              <!--
                关闭评论时只收起输入区，已发布的评论继续可读：
                评论是文章的一部分历史，开关不应该把它们一并抹掉。
              -->
              <p v-else class="comments__closed">{{ commentClosedHint }}</p>

              <div
                v-if="comments.length"
                :class="['comments__list', { 'comments__list--tight': !commentable }]"
              >
                <CommentList
                  :comments="comments"
                  :allow-reply="commentable"
                  @reply="(c) => (replyTo = c)"
                />
              </div>
              <div v-if="commentTotal > commentPageSize" class="comments__pager">
                <PaginationBar
                  :page="commentPage"
                  :page-size="commentPageSize"
                  :item-count="commentTotal"
                  :show-size-picker="false"
                  :show-quick-jumper="false"
                  @update:page="commentPage = $event"
                />
              </div>
            </section>
          </div>
        </div>
      </template>
      <n-empty v-else-if="!pending" description="文章不存在或未发布" style="margin-top: 80px">
        <template #extra>
          <n-button @click="$router.push('/')">返回列表</n-button>
        </template>
      </n-empty>
    </n-spin>
  </div>
</template>

<script setup>
import { MdCatalog, MdPreview } from 'md-editor-v3'
import 'md-editor-v3/lib/preview.css'
import '@/assets/styles/article.css'
import CommentList from '@/components/CommentList.vue'
import CommentForm from '@/components/CommentForm.vue'
import MyCommentPanel from '@/components/MyCommentPanel.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import { fetchComments, fetchPostBySlug } from '@/api/blog'
import { CODE } from '@/api/request'
import { userFacingError } from '@/utils/routeError'
import { formatDate, formatViews } from '@/utils/format'
import { siteSeoDescription, useSiteSetting } from '@/composables/useSiteSetting'
import { useBlogSeo } from '@/composables/useBlogSeo'

/** 正文预览器与大纲共用的 id，MdCatalog 靠它跟 MdPreview 建立联系 */
const catalogEditorId = 'blog-post-preview'
/** 大纲高亮 / 跳转的顶部偏移：吸顶导航高度 + 少量留白 */
const tocOffset = 76

const route = useRoute()
const site = useSiteSetting()
/**
 * 请求自身的地址（服务端取自请求头，浏览器取自 location）。
 * 必须在 setup 阶段取：`useRequestURL()` 依赖当前 Nuxt 实例，
 * 放进 computed getter 里再调用会抛 NUXT_E1001（getter 求值时已脱离组件上下文）。
 */
const requestUrl = useRequestURL()

/** 页面滚动容器由 layouts/default.vue 提供，大纲用它来监听滚动 / 定位跳转 */
const blogScrollEl = inject('blogScrollEl', null)
const scrollEl = computed(() => blogScrollEl?.value || undefined)

const slug = computed(() => String(route.params.slug || ''))

/**
 * 文章详情：服务端取数。
 *
 * <p>正文随首屏 HTML 一起返回，而不是在 `onMounted` 里 fetch —— 后者服务端
 * 只吐一个空壳，爬虫（尤其百度）什么都读不到。</p>
 *
 * <p>阅读数不由这个接口产生：后端 `publicDetailBySlug` 是纯读，计数由浏览器侧的
 * 访问埋点（{@link @/plugins/visit-tracker.client}）驱动，服务端只把「尚未回写数据库的
 * 增量」加进展示值。所以链接预取和 SSR 结果缓存都不会让它虚高或失真 ——
 * 它们本来就不带浏览器身份的埋点。
 * 详见后端 {@code BlogViewCounter} / {@code BlogViewFlushJob}。</p>
 */
const { data: postResult, status } = await useAsyncData(
  () => `blog:post:${slug.value}`,
  async () => {
    try {
      const res = await fetchPostBySlug(slug.value, { silent: true })
      return { ok: true, post: res.data }
    } catch (error) {
      return { ok: false, code: error.code, message: error.message }
    }
  },
  { default: () => ({ ok: true, post: null }) }
)

const pending = computed(() => status.value === 'pending')
const post = computed(() => (postResult.value?.ok ? postResult.value.post : null))

// 不存在 -> 真正的 404；后端异常 -> 502。两者都不该是「200 + 空页面」
if (postResult.value && !postResult.value.ok) {
  throw createError(toRouteError(postResult.value))
}
if (!pending.value && !post.value) {
  throw createError(userFacingError(404, '文章不存在或未发布'))
}

/**
 * 业务失败 -> 页面级 HTTP 错误。
 *
 * <p>`userFacingError` 负责把文案放在正确的位置并打上「可展示」标记，
 * 原因见 app/utils/routeError.js。</p>
 */
function toRouteError(result) {
  return result.code === CODE.NOT_FOUND
    ? userFacingError(404, result.message)
    : userFacingError(502, '文章加载失败，请稍后重试')
}

/* ---------- 评论 ---------- */

const replyTo = ref(null)
/** 递增即触发「我的评论」面板重新拉取 */
const myCommentRefresh = ref(0)
const commentPage = ref(1)
const commentPageSize = 10

/**
 * 评论同样走 SSR：评论是正文之外最实在的可索引文本，
 * 只在客户端拉就等于把这块内容整个让给 JS 渲染。
 */
const { data: commentsData } = await useAsyncData(
  () => `blog:comments:${post.value?.id || 0}:${commentPage.value}`,
  () =>
    fetchComments(
      post.value?.id,
      { pageNum: commentPage.value, pageSize: commentPageSize },
      { silent: true }
    )
      .then((res) => res.data)
      .catch(() => null),
  { default: () => null, immediate: Boolean(post.value?.id) }
)

const comments = computed(() => commentsData.value?.records || [])
const commentTotal = computed(() => Number(commentsData.value?.total) || 0)

/** 评论提交成功后回到第一页（新评论待审，不会立即出现在列表里） */
function onCommentSuccess() {
  replyTo.value = null
  commentPage.value = 1
  // 通知「我的评论」面板刷新，让作者立刻看到自己刚提交的待审评论
  myCommentRefresh.value++
}

/* ---------- 展示派生值 ---------- */

const displayTags = computed(() => {
  const names = post.value?.tagNames || []
  const slugs = post.value?.tagSlugs || []
  return names.map((name, i) => ({ name, slug: slugs[i] }))
})

const coverSrc = computed(() => post.value?.coverUrl || '')
const contentMd = computed(() => post.value?.content || '')

/**
 * 是否可以评论：站点总开关与单篇开关是「与」关系。
 * 站点总开关先判，这样关闭站点时不必等文章详情返回就已确定。
 */
const commentable = computed(
  () => site.value.commentEnabled === 1 && post.value?.allowComment === 1
)

/** 关闭原因提示：区分站点级与单篇级，便于访客判断 */
const commentClosedHint = computed(() =>
  site.value.commentEnabled === 1 ? '该文章已关闭评论' : '站点已关闭评论功能'
)

/** 正文标题列表（由 MdPreview 回传），非空才渲染左侧大纲 */
const headings = ref([])
const showToc = computed(() => headings.value.length > 0)

function handleCatalog(list) {
  headings.value = Array.isArray(list) ? list : []
}

/* ---------- SEO ---------- */

/** 标题：优先文章自身的 SEO 标题，其次「文章标题 · 站点名」 */
const seoTitle = computed(() => {
  const p = post.value
  if (!p) return ''
  return p.seoTitle || (p.title ? `${p.title} · ${site.value.siteName}` : site.value.siteName)
})

useBlogSeo({
  title: seoTitle,
  description: computed(
    () =>
      post.value?.seoDescription ||
      post.value?.summary ||
      siteSeoDescription(site.value)
  ),
  keywords: computed(() => post.value?.seoKeywords || undefined),
  image: coverSrc,
  type: 'article',
  // 结构化数据：让搜索结果有机会出富摘要（标题、时间、作者、缩略图）
  jsonLd: computed(() => {
    const p = post.value
    if (!p) return null
    return {
      '@context': 'https://schema.org',
      '@type': 'BlogPosting',
      headline: p.title,
      description: p.summary || p.seoDescription || '',
      image: coverSrc.value
        ? new URL(coverSrc.value, requestUrl.origin).href
        : undefined,
      // 后端是「yyyy-MM-dd HH:mm:ss」的本地时间（Asia/Shanghai），补上时区才是合法 ISO 8601
      datePublished: toIso(p.publishedAt),
      dateModified: toIso(p.publishedAt),
      author: { '@type': 'Person', name: p.authorName || site.value.siteName },
      publisher: { '@type': 'Organization', name: site.value.siteName },
      mainEntityOfPage: `${requestUrl.origin}/post/${p.slug}`
    }
  })
})

function toIso(value) {
  if (!value) return undefined
  return `${String(value).replace(' ', 'T')}+08:00`
}
</script>

<style scoped>
/*
 * 正文保持原有宽度并在容器内居中；大纲挂在正文左侧的页面留白（gutter）里，
 * 不参与正文布局，因此不会挤占中间的正文空间。
 */
.post-layout {
  position: relative;
}

.post-main {
  min-width: 0;
}

/* 窄屏两侧没有留白，不显示大纲 */
.post-toc {
  display: none;
  position: absolute;
  top: 0;
  bottom: 0;
  /* 右边缘落在正文左边缘之外 20px，即整块大纲位于正文左侧留白内 */
  right: calc(100% + 20px);
  width: 180px;
}

/* 容器最大宽度 920px，视口 ≥1320px 时左右留白足够容纳大纲 */
@media (min-width: 1320px) {
  .post-toc {
    display: block;
  }
}

/* 大纲在留白内吸顶跟随滚动 */
.post-toc__inner {
  position: sticky;
  top: calc(var(--layout-nav-height) + 24px);
  max-height: calc(100vh - var(--layout-nav-height) - 48px);
  overflow-y: auto;
  overscroll-behavior: contain;
  scrollbar-width: thin;
  scrollbar-color: rgba(0, 0, 0, 0.18) transparent;
}

.post-toc__title {
  margin-bottom: 10px;
  padding-left: 8px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--color-text-muted);
}

/* 品牌化目录配色：md-editor-v3 默认用 --md-color（在正文作用域外取不到），这里显式覆盖 */
.post-toc__catalog {
  position: relative;
}

.post-toc__catalog :deep(.md-editor-catalog-indicator) {
  background-color: var(--color-primary);
}

.post-toc__catalog :deep(.md-editor-catalog-link span) {
  font-size: 13px;
  line-height: 1.5;
  color: var(--color-text-secondary);
}

.post-toc__catalog :deep(.md-editor-catalog-link span:hover),
.post-toc__catalog :deep(.md-editor-catalog-active > span) {
  color: var(--color-primary);
}

.post-toc__catalog :deep(.md-editor-catalog-active > span) {
  font-weight: 600;
}

.post-header {
  margin-bottom: 16px;
}

.post-header h1 {
  font-family: var(--font-display);
  font-size: 34px;
  font-weight: 700;
  line-height: 1.25;
  margin-bottom: 16px;
  letter-spacing: -0.02em;
}

.post-header__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
  font-size: 13px;
  color: var(--color-text-muted);
}

.post-header__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
}

.tag-link {
  border: none;
  background: transparent;
  color: var(--color-text-secondary);
  font-size: 13px;
  cursor: pointer;
  padding: 0;
  font-family: inherit;
}

.tag-link:hover {
  color: var(--color-primary);
}

.post-cover {
  margin-bottom: 32px;
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.post-cover :deep(.n-image) {
  width: 100%;
  display: block;
}

/*
 * 封面完整展示，不裁剪。
 *
 * <p>这里曾用 `max-height: 420px` + `object-fit: cover`，把封面压成一条固定高度的横幅：
 * 宽度 872px 时按原图比例需要 452px 高，被压到 420px 后，多出来的部分由 cover 从
 * 上下两端各裁掉一截 —— 原图是完整的界面截图（顶栏和底栏都有内容），于是看起来
 * 「显示不全」。高度改由原图比例决定（height: auto），宽度撑满即可。</p>
 *
 * <p>代价：特别高的封面（竖图）会占掉较多首屏。这是有意的取舍 —— 封面按原样呈现，
 * 不为了排版整齐去裁用户上传的图。</p>
 */
.post-cover :deep(img) {
  display: block;
  width: 100%;
  height: auto;
  object-fit: contain;
}

.comments {
  margin-top: 48px;
  padding-top: 32px;
  border-top: 1px solid var(--color-border);
}

.comments h3 {
  font-family: var(--font-display);
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 20px;
}

.comments__list {
  margin-top: 28px;
}

/* 输入区被收起时列表紧跟提示，不需要为表单预留间距 */
.comments__list--tight {
  margin-top: 16px;
}

.comments__pager {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

.comments__pager :deep(.pagination-wrap) {
  justify-content: center;
  margin-top: 0;
  padding-top: 0;
}

.comments__closed {
  margin: 0;
  padding: 16px 20px;
  border-radius: var(--radius);
  background: var(--color-surface);
  color: var(--color-text-muted);
  font-size: 14px;
  text-align: center;
}

@media (max-width: 768px) {
  .post-header h1 {
    font-size: 26px;
  }
}
</style>
