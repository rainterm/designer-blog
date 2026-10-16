<template>
  <n-card :bordered="false" class="post-comments">
    <template #header>
      <div class="post-comments__title">文章评论</div>
    </template>
    <template #header-extra>
      <n-button v-if="postId" :loading="loading" @click="loadComments">刷新</n-button>
    </template>

    <!-- 新文章还没有 id，评论无处可挂，先提示而不是给一个点了会报错的空表单 -->
    <n-alert v-if="!postId" type="info" :bordered="false">
      文章保存后即可在这里查看评论、发表评论。
    </n-alert>

    <template v-else>
      <!--
        评论不可见时提前说明：后端同样会拒绝（见 BlogCommentService#addByOwner），
        但在这里讲清楚原因，比让用户点了才发现发不出去更好。
      -->
      <n-alert v-if="!canComment" type="warning" :bordered="false" class="post-comments__hint">
        {{ closedReason }}
      </n-alert>

      <!-- 发表评论（主动开一个新话题，不挂在任何评论下） -->
      <div v-if="canAdd" class="comment-compose">
        <n-input
          v-model:value="draft"
          type="textarea"
          :rows="3"
          maxlength="2000"
          show-count
          :placeholder="canComment ? `以「${ownerNickname}」的身份发表评论` : '当前不可发表评论'"
          :disabled="!canComment || adding"
        />
        <div class="comment-compose__foot">
          <span class="comment-compose__sign">署名：{{ ownerNickname }}</span>
          <n-button
            type="primary"
            :loading="adding"
            :disabled="!canComment || !draft.trim()"
            @click="handleAdd"
          >
            发表评论
          </n-button>
        </div>
      </div>

      <n-spin :show="loading">
        <div class="comment-list">
          <div v-if="!flat.length" class="comment-empty">还没有评论</div>
          <!--
            与前台同构：服务端下发两层树，这里摊平成带深度的列表再渲染，
            缩进、层级顺序、文案与样式都与 blog-web 的 CommentList.vue 对齐。
          -->
          <div
            v-for="item in flat"
            :key="item.id"
            class="comment-item"
            :style="{ marginLeft: item.depth * 24 + 'px' }"
          >
            <div class="comment-item__head">
              <span class="comment-item__name">{{ item.nickname }}</span>
              <!--
                置顶标识：与前台 CommentList.vue 一致，紧邻昵称。
                服务端已把置顶项排在该层靠前位置，标识用来说明它为什么排在最前面。
              -->
              <span v-if="isTop(item)" class="comment-item__top">置顶</span>
              <!-- 身份只看服务端 authorType，不看昵称文本，避免访客冒充 -->
              <span v-if="item.authorType === 1" class="comment-item__owner">博主</span>
              <!-- 第 2 层统一压平显示，用「回复 某某」标出这句话是回给谁的 -->
              <span v-if="item.replyToNickname" class="comment-item__reply-to">
                回复 <span class="comment-item__reply-to-name">{{ item.replyToNickname }}</span>
              </span>
              <span class="comment-item__time">{{ item.createTime }}</span>
              <span v-if="item.ipLocation" class="comment-item__location">
                {{ item.ipLocation }}
              </span>
              <span class="comment-item__status" :class="`is-status-${item.status}`">
                {{ statusOf(item.status).label }}
              </span>

              <!-- 操作与昵称同行、贴右；只显示图标，悬浮给出文字 -->
              <div v-if="editingId !== item.id" class="comment-item__actions">
                <!--
                  置顶用向上箭头。箭头类图标没有实心/描边两种字形可用，
                  所以「已置顶」靠按钮变主题色 + 提示语改成「取消置顶」来区分，
                  不再切换图标（原先在图钉的实心/描边上做区分，字形不好看）。
                -->
                <n-tooltip v-if="canTop" placement="top">
                  <template #trigger>
                    <n-button
                      quaternary
                      circle
                      :type="isTop(item) ? 'primary' : 'default'"
                      @click="handleTop(item)"
                    >
                      <template #icon>
                        <n-icon :component="ArrowUpOutline" />
                      </template>
                    </n-button>
                  </template>
                  {{ isTop(item) ? '取消置顶' : '置顶' }}
                </n-tooltip>
                <n-tooltip v-if="canEdit" placement="top">
                  <template #trigger>
                    <n-button quaternary circle type="primary" @click="startEdit(item)">
                      <template #icon>
                        <n-icon :component="CreateOutline" />
                      </template>
                    </n-button>
                  </template>
                  编辑
                </n-tooltip>
                <n-tooltip v-if="canAudit && item.status !== 1" placement="top">
                  <template #trigger>
                    <n-button
                      quaternary
                      circle
                      type="success"
                      @click="handleAudit(item, 1)"
                    >
                      <template #icon>
                        <n-icon :component="CheckmarkOutline" />
                      </template>
                    </n-button>
                  </template>
                  通过
                </n-tooltip>
                <n-tooltip v-if="canAudit && item.status !== 2" placement="top">
                  <template #trigger>
                    <n-button
                      quaternary
                      circle
                      type="warning"
                      @click="handleAudit(item, 2)"
                    >
                      <template #icon>
                        <n-icon :component="CloseOutline" />
                      </template>
                    </n-button>
                  </template>
                  拒绝
                </n-tooltip>
                <n-popconfirm v-if="canRemove" @positive-click="handleDelete(item)">
                  <template #trigger>
                    <n-button quaternary circle type="error">
                      <template #icon>
                        <n-icon :component="TrashOutline" />
                      </template>
                    </n-button>
                  </template>
                  确定删除该评论吗？
                </n-popconfirm>
              </div>
            </div>

            <!-- 编辑态：原位改正文，保存前不落库 -->
            <template v-if="editingId === item.id">
              <n-input
                v-model:value="editingDraft"
                type="textarea"
                :rows="3"
                maxlength="2000"
                show-count
              />
              <div class="comment-item__edit-foot">
                <n-button :disabled="saving" @click="cancelEdit">取消</n-button>
                <n-button
                  type="primary"
                  :loading="saving"
                  :disabled="!editingDraft.trim()"
                  @click="saveEdit(item)"
                >
                  保存
                </n-button>
              </div>
            </template>
            <p v-else class="comment-item__content">{{ item.content }}</p>
          </div>
        </div>
      </n-spin>

      <n-pagination
        v-if="total > pageSize"
        class="post-comments__pager"
        :page="pageNum"
        :page-size="pageSize"
        :item-count="total"
        @update:page="handlePage"
      />
    </template>
  </n-card>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useMessage } from 'naive-ui'
import {
  ArrowUpOutline,
  CheckmarkOutline,
  CloseOutline,
  CreateOutline,
  TrashOutline
} from '@vicons/ionicons5'
import {
  addComment,
  auditComment,
  deleteComments,
  getSetting,
  topComment,
  treeComments,
  updateComment
} from '@/modules/blog/api'
import { useUserStore } from '@/store/user'

const props = defineProps({
  /** 文章ID。为新文章（未保存）时为 null，此时评论无从挂载 */
  postId: { type: Number, default: null },
  /** 文章自身的「允许评论」开关，用于判断评论是否对访客可见 */
  allowComment: { type: Number, default: 1 }
})

const message = useMessage()
const userStore = useUserStore()

/**
 * 每页**顶级评论**条数（后端按顶级评论分页，每条带上它的全部回复），
 * 所以它是「话题数」而非评论条数。
 */
const pageSize = 10

const roots = ref([])
const total = ref(0)
const loading = ref(false)
const pageNum = ref(1)

const draft = ref('')
const adding = ref(false)

const editingId = ref(null)
const editingDraft = ref('')
const saving = ref(false)

/** 站点评论总开关与站长署名，来自「站点设置」 */
const siteCommentEnabled = ref(1)
const ownerNickname = ref('博主')

/**
 * 操作按钮逐个判权限（而不是用 v-perm 指令）。
 * 指令是在 mounted 时移除 DOM，套在 n-tooltip 这种多根节点组件上时
 * 拿到的 el 未必是外层元素；这里直接用 v-if 更可靠。
 */
const canEdit = computed(() => userStore.hasPerm('blog:comment:edit'))
const canAudit = computed(() => userStore.hasPerm('blog:comment:audit'))
const canRemove = computed(() => userStore.hasPerm('blog:comment:remove'))
const canTop = computed(() => userStore.hasPerm('blog:comment:top'))
const canAdd = computed(() => userStore.hasPerm('blog:comment:add'))
const canComment = computed(
  () => siteCommentEnabled.value === 1 && props.allowComment === 1
)
const closedReason = computed(() =>
  siteCommentEnabled.value !== 1
    ? '站点已关闭评论功能，发表后访客看不到。'
    : '该文章已关闭评论，发表后访客看不到。'
)

/**
 * 把两层评论树摊平成带缩进层级的列表，与前台 CommentList.vue 的算法一致。
 * 用栈迭代而非递归，避免脏数据造成的深层链把调用栈打爆。
 */
const flat = computed(() => {
  const result = []
  const stack = (roots.value || []).map((c) => ({ node: c, depth: 0 })).reverse()
  while (stack.length) {
    const { node, depth } = stack.pop()
    result.push({ ...node, depth })
    const children = node.children || []
    for (let i = children.length - 1; i >= 0; i--) {
      stack.push({ node: children[i], depth: depth + 1 })
    }
  }
  return result
})

const STATUS_MAP = {
  0: { label: '待审' },
  1: { label: '通过' },
  2: { label: '拒绝' }
}
const statusOf = (status) => STATUS_MAP[status] || { label: '-' }

/** 置顶标记：库里是 0/1，这里统一收敛成布尔，模板里少写一次比较 */
const isTop = (item) => item.isTop === 1

/**
 * 拉取评论树。
 *
 * <p>失败不做 toast：`@/api/request` 的响应拦截器已经统一提示过后端 msg，
 * 这里再提示一次会出现两条重复的错误条。</p>
 */
async function loadComments() {
  if (!props.postId) return
  loading.value = true
  try {
    const res = await treeComments({
      postId: props.postId,
      pageNum: pageNum.value,
      pageSize
    })
    roots.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

/** 站点开关与站长署名：拉取失败按「开启 + 默认署名」处理，不阻塞评论展示 */
async function loadSetting() {
  try {
    const res = await getSetting()
    siteCommentEnabled.value = res.data?.commentEnabled ?? 1
    ownerNickname.value = res.data?.ownerNickname || '博主'
  } catch {
    siteCommentEnabled.value = 1
    ownerNickname.value = '博主'
  }
}

async function handleAdd() {
  const content = draft.value.trim()
  if (!content) return
  adding.value = true
  try {
    await addComment({ postId: props.postId, content })
    message.success('评论已发表')
    draft.value = ''
    // 新评论 id 最大，而顶级评论按 id 升序分组，所以它在最后一页
    pageNum.value = Math.max(1, Math.ceil((total.value + 1) / pageSize))
    await loadComments()
  } finally {
    adding.value = false
  }
}

function startEdit(item) {
  editingId.value = item.id
  editingDraft.value = item.content || ''
}

function cancelEdit() {
  editingId.value = null
  editingDraft.value = ''
}

async function saveEdit(item) {
  const content = editingDraft.value.trim()
  if (!content) return
  saving.value = true
  try {
    await updateComment({ id: item.id, content })
    message.success('已保存')
    cancelEdit()
    await loadComments()
  } finally {
    saving.value = false
  }
}

async function handleAudit(item, status) {
  await auditComment({ id: item.id, status })
  message.success(status === 1 ? '已通过' : '已拒绝')
  await loadComments()
}

/**
 * 置顶 / 取消置顶。
 *
 * <p>置顶会改变顶级评论的先后，从而挪动分页边界：置顶的评论跳到第一页，
 * 原来第一页末尾那条相应后移。所以这里固定回到第一页，让用户直接看到结果。</p>
 */
async function handleTop(item) {
  const next = isTop(item) ? 0 : 1
  await topComment({ id: item.id, isTop: next })
  message.success(next === 1 ? '已置顶' : '已取消置顶')
  pageNum.value = 1
  await loadComments()
}

async function handleDelete(item) {
  await deleteComments([item.id])
  message.success('删除成功')
  // 删掉当前页最后一条时回退一页，避免停在空页上
  if (flat.value.length === 1 && pageNum.value > 1) {
    pageNum.value -= 1
  }
  await loadComments()
}

function handlePage(page) {
  pageNum.value = page
  loadComments()
}

/**
 * 文章切换时重置分页与编辑态并重新拉取。
 *
 * <p>用 immediate 兼作首次加载，省掉一个 onMounted。</p>
 */
watch(
  () => props.postId,
  () => {
    pageNum.value = 1
    cancelEdit()
    draft.value = ''
    if (props.postId) {
      loadComments()
    } else {
      roots.value = []
      total.value = 0
    }
  },
  { immediate: true }
)

onMounted(loadSetting)
</script>

<style scoped>
/*
 * 以下评论区的类名与样式刻意与前台 blog-web 的 CommentList.vue 保持一致，
 * 便于两处对照：布局、间距、圆角、字号、配色都对齐，
 * 仅把前台的 --radius-lg / --radius-full 换成后台已有的 --radius / 999px
 * （后台没有定义前者）。后台无暗色主题，故前台的写死色值 #f8f8f8、#444 也照搬。
 */
.post-comments {
  margin-top: 16px;
}

.post-comments__title {
  font-size: 16px;
  font-weight: 600;
}

.post-comments__hint {
  margin-bottom: 12px;
}

.comment-compose {
  margin-bottom: 16px;
}

.comment-compose__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
  font-size: 12px;
  color: var(--color-text-muted);
}

.comment-empty {
  background: #f8f8f8;
  border-radius: var(--radius);
  padding: 40px;
  text-align: center;
  border: 1px solid var(--color-border);
  color: var(--color-text-muted);
  font-size: 14px;
}

.comment-item {
  padding: 16px 0;
  border-bottom: 1px solid var(--color-border);
}

.comment-item__head {
  display: flex;
  gap: 12px;
  /* 前台是 baseline 对齐；这里同行还放着图标按钮，改成居中才不错位 */
  align-items: center;
  margin-bottom: 8px;
}

.comment-item__name {
  font-weight: 600;
  font-size: 14px;
}

/* 「博主」标识：实底胶囊，与「归属地」的浅描边胶囊同形；
   实色填充让它明显更重，于是「身份（有色块）」与「元信息（无色块）」一眼可分。 */
.comment-item__owner {
  flex-shrink: 0;
  padding: 1px 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
  line-height: 18px;
  letter-spacing: 0.02em;
  color: #fff;
  background: var(--color-primary);
  white-space: nowrap;
}

.comment-item__time {
  font-size: 12px;
  color: var(--color-text-muted);
}

/* 「置顶」标识：与「博主」的实底胶囊同形但改用描边+主题色，
   这样「身份（实底）」与「编辑标记（描边）」在视觉层级上仍分得开，
   同时又比纯元信息（时间、归属地）更醒目。与前台同名类样式一致。 */
.comment-item__top {
  flex-shrink: 0;
  padding: 1px 8px;
  border: 1px solid currentColor;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
  line-height: 18px;
  letter-spacing: 0.02em;
  color: var(--color-primary);
  white-space: nowrap;
}

.comment-item__location {
  font-size: 12px;
  color: var(--color-text-muted);
  border: 1px solid var(--color-border);
  border-radius: 999px;
  padding: 1px 8px;
  line-height: 1.6;
  white-space: nowrap;
}

.comment-item__reply-to {
  font-size: 12px;
  color: var(--color-text-muted);
  white-space: nowrap;
}

.comment-item__reply-to-name {
  color: var(--color-primary);
}

/* 审核状态：前台没有这个概念，沿用「元信息」的描边胶囊形制，
   只用颜色区分，避免与「博主」实底胶囊抢视觉。
   配色遵循项目约定：通过用主色、拒绝用 error 色，待审用弱化色（不新造色值）。 */
.comment-item__status {
  flex-shrink: 0;
  padding: 1px 8px;
  border: 1px solid currentColor;
  border-radius: 999px;
  font-size: 12px;
  line-height: 1.6;
  white-space: nowrap;
}

.comment-item__status.is-status-0 {
  color: var(--color-text-muted);
}

.comment-item__status.is-status-1 {
  color: var(--color-primary);
}

.comment-item__status.is-status-2 {
  color: var(--color-danger);
}

/* 操作贴昵称行右端；按钮本身是圆形图标，这里只负责对齐与间隔 */
.comment-item__actions {
  display: flex;
  align-items: center;
  flex-shrink: 0;
  gap: 2px;
  margin-left: auto;
}

.comment-item__content {
  margin: 0;
  font-size: 14px;
  line-height: 1.7;
  color: #444;
  white-space: pre-wrap;
  word-break: break-word;
}

.comment-item__edit-foot {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}

.post-comments__pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
</style>
