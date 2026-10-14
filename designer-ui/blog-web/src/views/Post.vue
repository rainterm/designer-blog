<template>
  <div class="post-page fade-in">
    <n-spin :show="loading">
      <template v-if="post">
        <div class="post-layout">
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

          <div class="post-main">
            <header class="post-header">
              <h1>{{ post.title }}</h1>
              <div class="post-header__meta">
                <span v-if="post.publishedAt">{{ formatDate(post.publishedAt) }}</span>
                <span v-if="post.authorName">{{ post.authorName }}</span>
                <span>{{ formatViews(post.viewCount) }} 阅读</span>
              </div>
              <div v-if="displayTags.length" class="post-header__tags">
                <button
                  v-for="t in displayTags"
                  :key="t.slug || t.name"
                  type="button"
                  class="tag-link"
                  @click="goTag(t)"
                >
                  #{{ t.name }}
                </button>
              </div>
            </header>

            <n-image-group>
              <div v-if="coverSrc" class="post-cover">
                <n-image :src="coverSrc" :alt="post.title" object-fit="cover" width="100%" />
              </div>

              <div class="article-body">
                <!--
                  直接渲染 md-editor-v3 预览器：与后台编辑器预览同一套渲染与样式
                  （代码块头部 / 复制按钮 / 折叠、图片点击放大均由组件自带）。
                  此前把 onHtmlChanged 的 HTML 再克隆成 VNode 渲染，会丢掉 .default-theme
                  祖先类，导致代码块样式、复制按钮交互全部失效。

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
                  @update:page="loadComments"
                />
              </div>
            </section>
          </div>
        </div>
      </template>
      <n-empty v-else-if="!loading" description="文章不存在或未发布" style="margin-top: 80px">
        <template #extra>
          <n-button @click="$router.push('/')">返回列表</n-button>
        </template>
      </n-empty>
    </n-spin>
  </div>
</template>

<script setup>
import { computed, inject, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { MdCatalog, MdPreview } from 'md-editor-v3'
import 'md-editor-v3/lib/preview.css'
import '@/styles/article.css'
import CommentList from '@/components/CommentList.vue'
import CommentForm from '@/components/CommentForm.vue'
import MyCommentPanel from '@/components/MyCommentPanel.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import { fetchComments, fetchPostBySlug } from '@/api/blog'
import { formatDate, formatViews } from '@/utils/format'
import { applySeoMeta } from '@/utils/seo'
import { site, siteSeoDescription } from '@/composables/useSiteSetting'

/** 正文预览器与大纲共用的 id，MdCatalog 靠它跟 MdPreview 建立联系 */
const catalogEditorId = 'blog-post-preview'
/** 大纲高亮 / 跳转的顶部偏移：吸顶导航高度 + 少量留白 */
const tocOffset = 76

const route = useRoute()
const router = useRouter()

/** 页面滚动容器由 BlogLayout 提供，大纲用它来监听滚动 / 定位跳转 */
const blogScrollEl = inject('blogScrollEl', null)
const scrollEl = computed(() => blogScrollEl?.value || undefined)

const loading = ref(false)
const post = ref(null)
const comments = ref([])
const replyTo = ref(null)
/** 递增即触发「我的评论」面板重新拉取 */
const myCommentRefresh = ref(0)
/** 评论分页（后端按顶级评论分页，每条带完整子树） */
const commentPage = ref(1)
const commentTotal = ref(0)
const commentPageSize = 10
/** 正文标题列表（由 MdPreview 回传），非空才渲染左侧大纲 */
const headings = ref([])

const showToc = computed(() => headings.value.length > 0)

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
const commentable = computed(() => site.commentEnabled === 1 && post.value?.allowComment === 1)

/** 关闭原因提示：区分站点级与单篇级，便于访客判断 */
const commentClosedHint = computed(() =>
  site.commentEnabled === 1 ? '该文章已关闭评论' : '站点已关闭评论功能'
)

function handleCatalog(list) {
  headings.value = Array.isArray(list) ? list : []
}

async function loadPost() {
  loading.value = true
  post.value = null
  comments.value = []
  replyTo.value = null
  headings.value = []
  commentPage.value = 1
  commentTotal.value = 0
  myCommentRefresh.value = 0
  try {
    const res = await fetchPostBySlug(route.params.slug)
    post.value = res.data
    applyPostSeo(post.value)
    if (post.value?.id) await loadComments()
  } catch {
    post.value = null
  } finally {
    loading.value = false
  }
}

/** 文章 SEO：优先文章自身字段，缺失回退站点默认（描述再回退摘要） */
function applyPostSeo(p) {
  if (!p) return
  const title = p.seoTitle || (p.title ? `${p.title} · ${site.siteName}` : '')
  applySeoMeta({
    title,
    keywords: p.seoKeywords || site.seoKeywords,
    description: p.seoDescription || p.summary || siteSeoDescription()
  })
}

async function loadComments(page = commentPage.value) {
  if (!post.value?.id) return
  commentPage.value = page
  try {
    const res = await fetchComments(post.value.id, {
      pageNum: page,
      pageSize: commentPageSize
    })
    comments.value = res.data?.records || []
    commentTotal.value = Number(res.data?.total) || 0
  } catch {
    comments.value = []
    commentTotal.value = 0
  }
}

/** 评论提交成功后回到第一页（新评论待审，不会立即出现在列表里） */
function onCommentSuccess() {
  replyTo.value = null
  loadComments(1)
  // 通知「我的评论」面板刷新，让作者立刻看到自己刚提交的待审评论
  myCommentRefresh.value++
}

function goTag(t) {
  if (t.slug) router.push({ name: 'tag', params: { slug: t.slug } })
}

watch(() => route.params.slug, loadPost)
onMounted(loadPost)
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
  margin-bottom: 32px;
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

.post-cover :deep(img) {
  width: 100%;
  max-height: 420px;
  object-fit: cover;
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
