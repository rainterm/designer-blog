<template>
  <div class="post-page fade-in">
    <n-spin :show="pending">
      <template v-if="post">
        <div class="post-layout">
          <!--
            大纲依赖正文渲染后的 DOM 测量，SSR 无意义。放进 ClientOnly 既避免服务端
            渲染时出错，也让它天然只在挂载后出现 —— 服务端与客户端首帧都是「没有大纲」，
            不存在 hydration 不一致。

            只在移动端（isMobile）把大纲收到正文上方；桌面端一律保留左侧目录列。
            桌面端布局是「正文保持原宽度 + 大纲浮在左侧留白」，正文位置不依赖大纲是否存在。
          -->
          <ClientOnly>
            <aside v-if="showToc && !isMobile" class="post-toc">
              <div class="post-toc__inner">
                <p class="post-toc__title">大纲</p>
                <PostOutline
                  class="post-toc__outline"
                  :items="tocItems"
                  :active-index="activeTocIndex"
                  @select="onTocSelect"
                />
              </div>
            </aside>
          </ClientOnly>

          <div class="post-main">
            <header class="post-header">
              <h1>{{ post.title }}</h1>
              <div class="post-header__meta">
                <!--
                  时间串由后端格式化（DateTimePattern.DATE_TIME = yyyy-MM-dd HH:mm:ss），
                  这里原样展示，不做切片、不做时区换算。
                -->
                <span v-if="post.publishedAt">发布于 {{ post.publishedAt }}</span>
                <!-- updated 也由后端判定（见 PublicPostVO.updated），前端只决定渲染与否 -->
                <span v-if="post.updated">更新于 {{ post.updateTime }}</span>
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

            <!--
              移动端的大纲：正文上方一块默认折叠的目录。
              桌面端不使用它 —— 那里大纲在左侧独立一列（见 .post-toc）。
            -->
            <ClientOnly>
              <details v-if="showToc && isMobile" ref="inlineTocEl" class="post-toc-inline">
                <summary class="post-toc-inline__summary">大纲</summary>
                <PostOutline
                  class="post-toc-inline__outline"
                  :items="tocItems"
                  :active-index="activeTocIndex"
                  @select="onTocSelect"
                />
              </details>
            </ClientOnly>

            <n-image-group>
              <div v-if="coverSrc" class="post-cover">
                <!--
                  object-fit 必须是 contain：封面按原图比例完整展示。
                  用 cover 会为了填满固定高度的容器而裁掉上下两端（图片一旦被判为
                  「横幅」，原图里的顶栏 / 底栏就会被切掉），这里以「看全」优先。
                -->
                <n-image :src="coverSrc" :alt="post.title" object-fit="contain" width="100%" />
              </div>

              <div class="article-body" ref="articleBodyEl" @click="onArticleClick">
                <!--
                  直接渲染 md-editor-v3 预览器：与后台编辑器预览同一套渲染与样式
                  （代码块头部 / 复制按钮 / 折叠、图片点击放大均由组件自带）。

                  SSR 的关键点在这里：MdPreview 的正文是在**渲染函数里**产出的
                  （unhead 之外的另一半收益），所以文章全文会出现在服务端返回的
                  HTML 里，爬虫不需要执行任何 JS 就能读到。

                  onGetCatalog 回传正文标题列表，用来决定左侧大纲是否显示。
                  md-heading-id 换成 GitHub 风格的 slug，让正文里手写的目录锚点能对上，
                  详见 @/utils/headingAnchor。
                -->
                <MdPreview
                  :id="catalogEditorId"
                  :model-value="contentMd"
                  language="zh-CN"
                  :theme="isDark ? 'dark' : 'light'"
                  :md-heading-id="headingId"
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
import { MdPreview } from 'md-editor-v3'
import 'md-editor-v3/lib/preview.css'
import { useMessage } from 'naive-ui'
import '@/assets/styles/article.css'
import CommentList from '@/components/CommentList.vue'
import CommentForm from '@/components/CommentForm.vue'
import MyCommentPanel from '@/components/MyCommentPanel.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import PostOutline from '@/components/PostOutline.vue'
import { fetchComments, fetchPostBySlug } from '@/api/blog'
import { CODE } from '@/api/request'
import { userFacingError } from '@/utils/routeError'
import { formatViews } from '@/utils/format'
import { headingId, resolveHeading } from '@/utils/headingAnchor'
import { isRepoRelativeLink } from '@/utils/articleLinks'
import { normalizeMarkdownParagraphs } from '@/utils/normalizeMarkdownParagraphs'
import { siteSeoDescription, useSiteSetting } from '@/composables/useSiteSetting'
import { useBlogSeo } from '@/composables/useBlogSeo'
import { useColorMode } from '@/composables/useColorMode'

/** 正文预览器与大纲共用的 id（MdPreview 的内层 DOM 会带着它，便于定位预览容器） */
const catalogEditorId = 'blog-post-preview'
/**
 * 跳转时的顶部留白：吸顶导航高度 + 少量留白。
 * 大纲点击与正文内的锚点跳转共用，两者落点才一致。
 */
const stickyOffset = 76

/**
 * 大纲「当前标题」的判定线。
 *
 * <p>比 {@link stickyOffset} 略大一点：点击跳转把标题滚到 scrollTop 对应的 76px 处，
 * 实测落点还会多 1px 左右（亚像素取整）。判定线正好取 76 的话，标题会落在线上，
 * `<=` 的比较刚好成立，但只要多 1px 就会退回上一个标题 —— 高亮就「不跟着点击走」。
 * 留 8px 余量，既容下取整误差，也让高亮切换发生在吸顶导航下沿稍往下的位置。</p>
 */
const tocActiveOffset = stickyOffset + 8

/** 大纲最多展示到几级标题（h1–h3），再深就太碎 */
const TOC_MAX_DEPTH = 3

const route = useRoute()
const site = useSiteSetting()
const { isDark } = useColorMode()
const message = useMessage()
/**
 * 请求自身的地址（服务端取自请求头，浏览器取自 location）。
 * 必须在 setup 阶段取：`useRequestURL()` 依赖当前 Nuxt 实例，
 * 放进 computed getter 里再调用会抛 NUXT_E1001（getter 求值时已脱离组件上下文）。
 */
const requestUrl = useRequestURL()

/** 页面滚动容器由 layouts/default.vue 提供，大纲用它来监听滚动 / 定位跳转 */
const blogScrollEl = inject('blogScrollEl', null)

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
/**
 * 正文交给 MdPreview 前先做纯文本换行规范化：
 * 单回车分段的粘贴稿会变成真正的 {@code <p>}，而不是一堵 {@code <br>} 墙。
 * 规则见 {@link normalizeMarkdownParagraphs}。
 */
const contentMd = computed(() => normalizeMarkdownParagraphs(post.value?.content || ''))

/**
 * 最后更新时间要不要提示，由后端判定（{@link PublicPostVO#getUpdated()}）。
 *
 * <p>这里刻意不做任何时间解析或比较：格式由 {@code DateTimePattern} 统一，
 * 「是否算更新过」也依赖发布与更新两个字段的先后关系 —— 两者都属于数据语义，
 * 放在前端会让「同一天算不算更新」这类规则随页面各写一套。</p>
 */

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

/** 正文标题列表（由 MdPreview 回传），非空才渲染大纲 */
const headings = ref([])
const showToc = computed(() => headings.value.length > 0)

/**
 * 是否移动端 —— 决定大纲放在哪里，**不**决定它在不在。
 *
 * <p>桌面端（宽度 &gt; 768px）大纲始终占据正文左侧独立一列；只有移动端才收进
 * 正文上方的折叠块。之前用的是「视口 ≥1320px 才显示左侧大纲」，
 * 1280px 的笔记本（100% 缩放）正好差一点，于是左侧目录直接消失 —— 那不是在判断
 * 设备形态，而是在赌用户窗口够不够宽。</p>
 *
 * <p>断点与全站一致（{@code --layout-page-padding} 的移动端断点也是 768），
 * 保证「移动端」在本站只有一个定义。取值放 JS 而不是 CSS 媒体查询，是因为它决定
 * **渲染哪一份 DOM**：两份大纲同时存在会有两条同样的滚动监听、两处高亮。
 * 用 v-if 只挂载其中一份最干净。</p>
 *
 * <p>服务端固定渲染 false：桌面端大纲本来就只出现在客户端（ClientOnly），正文位置
 * 不依赖它，所以不存在 hydration 不一致；移动端的 CSS 媒体查询负责把正文占满整行。</p>
 */
const MOBILE_QUERY = '(max-width: 768px)'
const isMobile = ref(
  import.meta.client ? window.matchMedia(MOBILE_QUERY).matches : false
)
let mobileQuery = null

function handleViewportChange(event) {
  isMobile.value = event.matches
}

onMounted(() => {
  mobileQuery = window.matchMedia(MOBILE_QUERY)
  isMobile.value = mobileQuery.matches
  mobileQuery.addEventListener('change', handleViewportChange)
})

onBeforeUnmount(() => {
  mobileQuery?.removeEventListener('change', handleViewportChange)
})

function handleCatalog(list) {
  headings.value = Array.isArray(list) ? list : []
}

/* ---------- 大纲：数据 / 高亮 / 点击 ---------- */

/** 正文容器：既作为锚点查找范围，也用来做点击事件委托 */
const articleBodyEl = ref(null)

/** 移动端折叠大纲的容器，点击后收起 */
const inlineTocEl = ref(null)

/**
 * 大纲条目：由 MdPreview 回传的标题列表转换而成。
 *
 * <p>id 用 {@link headingId} 生成，与正文标题的 id（`md-heading-id`）同一个函数，
 * 所以英文标题（RBAC / SSO）也能对上 —— 之前直接拿标题原文当 id 去查，
 * 正文里的 id 是小写 slug，`RBAC` 查不到，点了不滚。</p>
 *
 * <p>depth 是相对最小层级的缩进，不写死 h1=0：有的文章直接从 h2 开始，
 * 按最小层级归一化后，目录不会整体缩进一大截。</p>
 */
const tocItems = computed(() => {
  const list = headings.value.filter((h) => Number(h.level) <= TOC_MAX_DEPTH)
  if (!list.length) return []
  const baseLevel = Math.min(...list.map((h) => Number(h.level)))
  return list.map((h) => ({
    id: headingId(h.text),
    text: h.text,
    depth: Number(h.level) - baseLevel
  }))
})

/** 当前高亮项下标；滚动与点击都只改这一个值 */
const activeTocIndex = ref(0)

/** 在正文里按 id 找标题元素；找不到（id 被改写等）再用归一化兜底 */
function tocHeadingEl(item) {
  const root = articleBodyEl.value
  if (!root || !item) return null
  return root.querySelector(`[id="${CSS.escape(item.id)}"]`) || resolveHeading(item.id, root)
}

/**
 * 重算当前高亮项：取「最后一个已经到达判定线的标题」。
 *
 * <p>判定线与点击跳转用同一个 `tocActiveOffset`，因此点完立刻算一次就必然
 * 命中被点的那一项，不会像第三方目录那样出现「点完高亮还停在上一条」。</p>
 */
function computeActiveToc() {
  const scroller = blogScrollEl?.value
  const items = tocItems.value
  if (!scroller || !items.length) return
  // 程序滚动进行中，高亮由 onTocSelect 决定，别让位置计算把它拉回去
  if (tocScrollLocked) return

  // 已经滚到底：最后几节通常不够高，没法把标题顶到判定线，
  // 这时按位置算会一直停在前一条，读者点了最后一项却看不到高亮
  const atBottom =
    scroller.scrollTop + scroller.clientHeight >= scroller.scrollHeight - 2
  if (atBottom) {
    activeTocIndex.value = items.length - 1
    return
  }

  const scrollerTop = scroller.getBoundingClientRect().top
  let next = 0
  for (let i = 0; i < items.length; i += 1) {
    const el = tocHeadingEl(items[i])
    if (!el) continue
    if (el.getBoundingClientRect().top - scrollerTop <= tocActiveOffset) next = i
    // 标题在文档里是顺序排列的，第一个还没到判定线的后面就都不用看了
    else break
  }
  activeTocIndex.value = next
}

/**
 * 滚动事件很密，做个尾部节流：每 80ms 最多算一次。
 *
 * <p>刻意不用 requestAnimationFrame：页面在后台标签页时 rAF 会被暂停，
 * 定时器仍会执行；而且这里只需要「滚动停下来后高亮是准的」，不需要逐帧对齐。</p>
 */
let activeTocTimer = 0
function scheduleActiveToc() {
  // 程序触发的滚动期间不重算（原因见 {@link onTocSelect}），只延后解锁的判定
  if (tocScrollLocked) {
    armTocScrollUnlock()
    return
  }
  if (activeTocTimer) return
  activeTocTimer = setTimeout(() => {
    activeTocTimer = 0
    computeActiveToc()
  }, 80)
}

/**
 * 是否正处在「点击大纲触发的滚动」中。
 *
 * <p>这一小段里滚动监听必须闭嘴，否则高亮会来回闪：点击时先把高亮置为被点项，
 * 但动画才刚开始、标题位置还没动，监听立刻按位置算出的是**上一条**，
 * 高亮被打回去；随后动画推进、判定线依次经过中间标题，高亮又一路跳回来 ——
 * 表现就是点一下先闪到旧项、再扫过一串、最后才停。位移覆盖两屏以上时更明显。</p>
 *
 * <p>所以动画期间保持高亮不动，等真正滚到落点再解锁并重算一次（判定见
 * {@link finishTocScroll}）。落点与判定线本来就对齐，重算结果就是被点项，
 * 肉眼看不到变化；万一用户在中途自己滚动了（滚轮 / 触摸 / 拖动滚动条），
 * 会立即解锁并纠正高亮。</p>
 */
let tocScrollLocked = false
let tocScrollUnlockTimer = 0
/** 锁定期间被点的条目下标，解锁时用来兜底（见下） */
let tocLockIndex = -1
/** 本次程序滚动的目标位置；用来判断动画是否真的到位 */
let tocTargetTop = 0

/** 多久没有 scroll 事件算「停下了」 */
const TOC_SCROLL_SETTLE_MS = 120
/**
 * 锁定最长持续时间。
 *
 * <p>兜底用：目标位置因为页面高度变化等原因滚不到时，不能让高亮永远停在被点项上。
 * 取 2s —— 比浏览器平滑滚动的典型时长（≤700ms）宽松，又短到用户不会觉得卡住。</p>
 */
const TOC_SCROLL_MAX_LOCK_MS = 2000
let tocLockStartedAt = 0

function armTocScrollUnlock() {
  clearTimeout(tocScrollUnlockTimer)
  tocScrollUnlockTimer = setTimeout(finishTocScroll, TOC_SCROLL_SETTLE_MS)
}

/**
 * 尝试结束程序滚动的锁定。
 *
 * <p>**必须等滚动到位**，不能只看「有没有 scroll 事件」：浏览器的平滑滚动
 * 不一定立刻起步（实测点击后会有上百毫秒的静止），这段时间里没有任何事件，
 * 若就此解锁，位置重算拿到的是旧位置 —— 高亮先弹回上一条，再随动画扫回来，
 * 正是「来回闪」的来源。所以这里同时校验落点，没到就继续等。</p>
 */
function finishTocScroll() {
  const scroller = blogScrollEl?.value
  if (!tocScrollLocked) return
  const notArrived = scroller && Math.abs(scroller.scrollTop - tocTargetTop) > 4
  const timedOut = performance.now() - tocLockStartedAt > TOC_SCROLL_MAX_LOCK_MS
  if (notArrived && !timedOut) {
    armTocScrollUnlock()
    return
  }

  tocScrollLocked = false
  const lockedIndex = tocLockIndex
  tocLockIndex = -1
  computeActiveToc()
  /**
   * 兜底：被点项还在可视区里，就以它为准。
   *
   * <p>两种情况会让位置重算落到别的条目上：一是点到靠后的标题时页面已经到底，
   * 滚动被 `scrollHeight` 截断，标题到不了判定线；二是触底兜底规则直接把高亮
   * 给了最后一条。这时被点项明明就在眼前，高亮却跳走，看着仍像"没跟着点"。</p>
   */
  const el = lockedIndex >= 0 ? tocHeadingEl(tocItems.value[lockedIndex]) : null
  if (scroller && el) {
    const top = el.getBoundingClientRect().top - scroller.getBoundingClientRect().top
    if (top >= 0 && top <= scroller.clientHeight - 48) activeTocIndex.value = lockedIndex
  }
}

/**
 * 用户自己滚动时立刻解锁，别让程序滚动的静默期压住手动滚动的高亮更新。
 */
function releaseTocScrollLock() {
  if (!tocScrollLocked) return
  tocScrollLocked = false
  tocLockIndex = -1
  clearTimeout(tocScrollUnlockTimer)
  computeActiveToc()
}

/**
 * 点击大纲条目。
 *
 * <p>先把自己的高亮置为被点项，再滚动并进入锁定状态 —— 等滚动结束才交给
 * 位置重算（原因见 {@link tocScrollLocked}）。</p>
 */
function onTocSelect(item, index) {
  activeTocIndex.value = index
  const el = tocHeadingEl(item)
  if (!el) return
  tocScrollLocked = true
  tocLockIndex = index
  tocLockStartedAt = performance.now()
  armTocScrollUnlock()
  const target = scrollHeadingIntoView(el)
  tocTargetTop = target ?? blogScrollEl?.value?.scrollTop ?? 0
  // 与正文内锚点一致：只改地址栏 hash，不触发路由变化（否则布局会把页面滚回顶部）
  history.replaceState(history.state, '', `#${item.id}`)
  // 移动端大纲在正文上方，点完收起，免得挡住刚滚到的内容
  if (inlineTocEl.value) inlineTocEl.value.open = false
}

watch(tocItems, () => nextTick(computeActiveToc))

/** 滚动容器切了（理论上不会，但别假设）就连监听一起跟着换 */
watch(
  () => blogScrollEl?.value,
  (scroller, _prev, onCleanup) => {
    if (!scroller) return
    scroller.addEventListener('scroll', scheduleActiveToc, { passive: true })
    // 用户自己滚（滚轮 / 触摸 / 键盘 / 拖动滚动条）时立刻结束程序滚动的锁定
    scroller.addEventListener('wheel', releaseTocScrollLock, { passive: true })
    scroller.addEventListener('touchmove', releaseTocScrollLock, { passive: true })
    scroller.addEventListener('pointerdown', releaseTocScrollLock, { passive: true })
    scroller.addEventListener('keydown', releaseTocScrollLock)
    nextTick(computeActiveToc)
    onCleanup(() => {
      scroller.removeEventListener('scroll', scheduleActiveToc)
      scroller.removeEventListener('wheel', releaseTocScrollLock)
      scroller.removeEventListener('touchmove', releaseTocScrollLock)
      scroller.removeEventListener('pointerdown', releaseTocScrollLock)
      scroller.removeEventListener('keydown', releaseTocScrollLock)
    })
  },
  { immediate: true }
)

onBeforeUnmount(() => {
  if (activeTocTimer) clearTimeout(activeTocTimer)
  clearTimeout(tocScrollUnlockTimer)
  window.removeEventListener('resize', scheduleActiveToc)
})

onMounted(() => {
  // 窗口变宽变窄会改变排版与标题位置，高亮得跟着重算
  window.addEventListener('resize', scheduleActiveToc, { passive: true })
})

/* ---------- 正文内的锚点跳转 ---------- */

/**
 * 把某个标题滚到可视区（让开吸顶导航）。
 *
 * <p>**不能**靠浏览器的原生片段跳转（`<a href="#foo">` 的默认行为）：全站真正的滚动容器是
 * `layouts/default.vue` 的 `.blog-scroll`，而 `html / body / #__nuxt` 都是
 * `overflow: hidden`（见 `assets/styles/index.css`）。document 本身不可滚动，
 * 浏览器找不到可滚动的祖先，结果是地址栏 hash 变了、页面一动不动。</p>
 *
 * <p>所以这里自己算出相对于滚动容器的位置再滚。</p>
 */
function scrollHeadingIntoView(el, behavior = 'smooth') {
  const scroller = blogScrollEl?.value
  if (!scroller || !el) return null
  // 夹到可滚范围：点到靠后的标题时页面可能滚不到计算出来的位置
  const target = Math.min(
    scroller.scrollHeight - scroller.clientHeight,
    Math.max(
      0,
      scroller.scrollTop +
        (el.getBoundingClientRect().top - scroller.getBoundingClientRect().top) -
        stickyOffset
    )
  )
  /**
   * 远处的标题直接跳，不做平滑滚动。
   *
   * <p>平滑滚动横跨几千像素要一两秒，动画期间整页都在飞速掠过，观感很差。
   * 两屏以内的小跳跃保留动画，既有反馈又不会等太久。</p>
   */
  const distance = Math.abs(target - scroller.scrollTop)
  const mode = behavior === 'smooth' && distance > scroller.clientHeight * 2 ? 'auto' : behavior
  scroller.scrollTo({ top: target, behavior: mode })
  return target
}

/**
 * 正文内的链接点击处理（委托在正文容器上），分两类接管：
 *
 * <p>1. **页内锚点** `#xxx`：滚动到对应标题（原因见 {@link scrollHeadingIntoView}）。
 * 只拦「能在正文里找到目标」的锚点，找不到就放行。</p>
 *
 * <p>2. **仓库内相对路径** `docs/开发规范.md` 这类：博客站内没有对应文件，
 * 放行只会 404，所以直接禁止跳转，并提示文件在代码仓库里（判定见
 * `@/utils/articleLinks`）。</p>
 *
 * <p>其余链接（`https://…`、`mailto:`、站内根路径等）一律不干预，交给浏览器默认行为 ——
 * 包括 md-editor-v3 图片放大用的那些 `<a>`。</p>
 *
 * <p>锚点跳转时 URL 用 `history.replaceState` 更新而不是走 `router.push`：布局里 watch 了
 * `route.fullPath`，一旦路由变化就把 `.blog-scroll` 滚回顶部 —— 那会和这里的滚动打架，
 * 表现为「点一下先跳过去、马上又弹回顶部」。`replaceState` 不通知 vue-router，
 * 既保住了可分享的地址栏 hash，又不触发那次回顶。用 replace 而非 push 是为了不让
 * 每一次目录点击都塞一条历史记录，返回时直接回到上一页更符合直觉。</p>
 */
function onArticleClick(event) {
  const link = event.target?.closest?.('a[href]')
  if (!link) return
  const href = link.getAttribute('href') || ''

  // 仓库内相对路径（如 docs/开发规范.md）：博客站内没有这些文件，
  // 放行只会跳到 404，所以拦下并说明原因
  if (isRepoRelativeLink(href)) {
    event.preventDefault()
    message.info('该文件位于代码仓库中，博客站内无法打开')
    return
  }

  if (!href.startsWith('#')) return

  const target = resolveHeading(href, articleBodyEl.value)
  // 解析不到就别拦：让浏览器按原样试一次，比静默什么都不做更好排查
  if (!target) return

  event.preventDefault()
  scrollHeadingIntoView(target)
  history.replaceState(history.state, '', href)
}

/**
 * 首屏深链（分享出去带 `#锚点` 的地址）也要落到对应标题。
 *
 * <p>**不能**放在 `onMounted`：那时 MdPreview 的正文还没进 DOM —— 标题及其 id 都是
 * 客户端渲染完才生成的，`resolveHeading` 只会拿到 null，深链就静默失效了。
 * 等预览器回传标题列表（`headings` 非空，说明正文已渲染）再补跳一次，且只跳一次，
 * 避免后续任何重渲染又跳一回。</p>
 */
const pendingHash = ref(
  typeof route.hash === 'string' && route.hash.length > 1 ? route.hash : ''
)

watch(headings, async (list) => {
  if (!pendingHash.value || !list.length) return
  const hash = pendingHash.value
  pendingHash.value = ''
  await nextTick()
  const target = resolveHeading(hash, articleBodyEl.value)
  if (!target) return
  scrollHeadingIntoView(target, 'auto')
  // 深链是直接跳过去的，没有触发滚动事件序列，高亮得手动对齐一次
  computeActiveToc()
})

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
      // dateModified 用真正的编辑时间；没编辑过（或旧数据缺列）时回退发布时间
      dateModified: toIso(p.updateTime) || toIso(p.publishedAt),
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
 * 桌面端：正文保持居中的原有宽度，大纲放在正文左侧的页面留白里 ——
 * 大纲**不占用正文宽度**，正文该多宽还是多宽。
 *
 * <p>以前大纲的位置靠「视口 ≥ 1320px 才挂留白」判断（写在 JS 的 media query 里），
 * 1280px 的笔记本在 100% 缩放下正好差一点，大纲就被当成窄屏收走：
 * 同一个人 100% 看不到、90% 又能看到。这其实是在拿窗口宽度猜设备形态。</p>
 *
 * <p>现在的划分只按设备：移动端（≤768px，与全站断点一致）收进正文上方的折叠块；
 * 桌面端一律保留左侧大纲。桌面内部再分两档解决「留白不够」：
 * 视口够宽（≥1160px，820 正文两侧各剩 170px 以上）时大纲真的落在留白里；
 * 再窄一些的桌面就让正文让出 184px 给大纲，保证它不会被裁到屏幕外。</p>
 */
.post-layout {
  position: relative;
}

.post-main {
  min-width: 0;
}

.post-toc {
  width: 168px;
}

/*
 * 宽屏：正文仍居中 820px，大纲整体落在正文左边缘之外 16px 处，
 * 完全位于页面留白中，不参与正文布局。
 *
 * <p>断点 1160 的由来：正文左边缘到视口左侧的距离 = (视口 - 820) / 2 + 24（容器内边距），
 * 要放下 168px 大纲 + 16px 间距共 184px，视口需 ≥ 1140px；再留 20px 余量
 * （抵消滚动条占宽），取整到 1160。</p>
 */
@media (min-width: 1160px) {
  .post-toc {
    position: absolute;
    top: 0;
    bottom: 0;
    right: calc(100% + 16px);
  }
}

/*
 * 中等桌面（769px–1159px）：页面留白不足，正文让出左侧 184px（168 + 16 间距）。
 * 这是唯一会压缩正文的情形，且只在桌面窗口确实放不下大纲时发生。
 */
@media (min-width: 769px) and (max-width: 1159px) {
  .post-layout {
    padding-left: 184px;
  }

  .post-toc {
    position: absolute;
    top: 0;
    bottom: 0;
    left: 0;
  }
}

/*
 * 移动端大纲：正文里的一块可折叠目录（桌面端不渲染，见模板里的 v-if）。
 *
 * <p>默认收起 —— 移动端首屏应该先看到标题与正文；展开后高度受限，
 * 长文章的目录不会把正文推到很下面。</p>
 */
.post-toc-inline {
  margin-bottom: 20px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-surface);
  overflow: hidden;
}

.post-toc-inline__summary {
  padding: 10px 16px;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
  cursor: pointer;
  list-style: none;
  user-select: none;
}

/* 自定义展开箭头，顺带去掉各浏览器默认的三角（list-style 对它无效） */
.post-toc-inline__summary::-webkit-details-marker {
  display: none;
}

.post-toc-inline__summary::before {
  content: '▸';
  display: inline-block;
  width: 1em;
  color: var(--color-text-muted);
  transition: transform 0.15s;
}

.post-toc-inline[open] > .post-toc-inline__summary::before {
  transform: rotate(90deg);
}

.post-toc-inline .post-toc-inline__outline {
  max-height: 40vh;
  padding: 0 16px 12px;
  overflow-y: auto;
  overscroll-behavior: contain;
}

/* 大纲在大纲列内吸顶跟随滚动 */
.post-toc__inner {
  position: sticky;
  top: calc(var(--layout-nav-height) + 24px);
  max-height: calc(100vh - var(--layout-nav-height) - 48px);
  overflow-y: auto;
  overscroll-behavior: contain;
  scrollbar-width: thin;
  scrollbar-color: var(--color-scrollbar) transparent;
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
  /* 移动端：大纲改由正文上方的 .post-toc-inline 承担，正文占满整行 */
  .post-layout {
    padding-left: 0;
  }

  .post-header h1 {
    font-size: 26px;
  }
}
</style>
