<template>
  <div class="page-container">
    <SearchBar :pager="pager">
      <template #fields>
        <n-form-item label="关键词">
          <n-input v-model:value="query.keyword" placeholder="昵称 / 内容" clearable />
        </n-form-item>
        <n-form-item label="状态">
          <n-select v-model:value="query.status" :options="statusOptions" placeholder="全部" clearable />
        </n-form-item>
      </template>
    </SearchBar>

    <PageTable :pager="pager" :columns="columns">
      <template #toolbar-right>
        <n-space>
          <n-button
            v-perm="'blog:comment:remove'"
            type="error"
            :disabled="!checkedRowKeys.length"
            @click="handleBatchDelete"
          >
            批量删除
          </n-button>
        </n-space>
      </template>
    </PageTable>

    <Dialog
      v-model:show="replyVisible"
      title="回复评论"
      :loading="replySubmitting"
      confirm-text="回复"
      @confirm="submitReply"
    >
      <!-- 引用被回复的评论，避免在平铺列表里回复错对象 -->
      <div v-if="replyTarget" class="reply-quote">
        <span class="reply-quote__label">回复</span>
        <span class="reply-quote__name">{{ replyTarget.nickname }}</span>
        <p class="reply-quote__content">{{ replyTarget.content }}</p>
      </div>

      <n-form
        ref="replyFormRef"
        :model="replyForm"
        :rules="replyRules"
        label-placement="left"
        label-width="80"
      >
        <!-- 署名不在这里填：统一用「站点设置 → 站长署名」，避免同一个人出现多个署名 -->
        <n-form-item label="署名">
          <span class="reply-owner">{{ ownerNickname }}</span>
        </n-form-item>
        <n-form-item label="回复内容" path="content">
          <n-input
            v-model:value="replyForm.content"
            type="textarea"
            :rows="4"
            maxlength="2000"
            show-count
            placeholder="请输入回复内容"
          />
        </n-form-item>
      </n-form>
    </Dialog>
  </div>
</template>

<script setup>
import { computed, h, onMounted, ref } from 'vue'
import { NTag, useMessage } from 'naive-ui'
import { ArrowUndoOutline, CheckmarkOutline, CloseOutline, TrashOutline } from '@vicons/ionicons5'
import { hActionBtn, hActionConfirm, hActionGroup } from '@/utils/actionBtn'
import { auditComment, deleteComments, pageComments, getSetting, replyComment } from '@/modules/blog/api'
import { useActionLock } from '@/composables/useActionLock'
import { usePagination } from '@/composables/usePagination'
import { useBatchDelete } from '@/composables/useBatchDelete'
import { useFormDialog } from '@/composables/useFormDialog'
import { useUserStore } from '@/store/user'

const message = useMessage()
const userStore = useUserStore()
const { locked, busy, run } = useActionLock()

const pager = usePagination((q) => pageComments(q), {
  defaults: {
    keyword: '',
    status: null
  }
})
const { query, loadData, checkedRowKeys } = pager
const { handleBatchDelete } = useBatchDelete(pager, deleteComments, {
  confirm: (n) => `确定删除选中的 ${n} 条评论吗？`,
  positiveText: '删除'
})

/** 当前正在回复的评论，仅用于弹窗里展示原文 */
const replyTarget = ref(null)

/**
 * 站点评论总开关：关闭时回复按钮置灰。
 * 后端同样会拒绝，这里只是提前给出可理解的反馈，避免点了才报错。
 */
const commentEnabled = ref(1)
const replyDisabled = computed(() => commentEnabled.value !== 1)
const replyTip = computed(() => (replyDisabled.value ? '站点已关闭评论功能' : '回复'))

/** 站长署名（站点设置）：回复固定用它，弹窗里只做展示，不给编辑入口 */
const ownerNickname = ref('博主')

const {
  visible: replyVisible,
  submitting: replySubmitting,
  formRef: replyFormRef,
  form: replyForm,
  openCreate: openReplyDialog,
  submit: submitReply
} = useFormDialog({
  defaults: { id: null, content: '' },
  async onSubmit(payload) {
    // 不带署名：服务端统一取「站点设置 → 站长署名」
    await replyComment({ id: payload.id, content: payload.content })
    message.success('回复成功')
    loadData()
  }
})

const replyRules = {
  content: [{ required: true, message: '请输入回复内容', trigger: 'blur' }]
}

function handleReply(row) {
  replyTarget.value = row
  openReplyDialog({ id: row.id })
}

const statusOptions = [
  { label: '待审', value: 0 },
  { label: '通过', value: 1 },
  { label: '拒绝', value: 2 }
]

const statusMap = {
  0: { label: '待审', type: 'warning' },
  1: { label: '通过', type: 'success' },
  2: { label: '拒绝', type: 'error' }
}

const columns = computed(() => [
  { type: 'selection', width: 44 },
  { title: 'ID', key: 'id', width: 70 },
  { title: '文章', key: 'postTitle', width: 160, ellipsis: { tooltip: true } },
  {
    title: '昵称',
    key: 'nickname',
    width: 130,
    render: (row) =>
      h('span', { style: 'display:inline-flex;align-items:center;gap:6px;' }, [
        h('span', null, row.nickname),
        // 平铺列表里看不出层级，用标签标出这是回复
        row.parentId ? h(NTag, { size: 'small', bordered: false, type: 'info' }, { default: () => '回复' }) : null
      ])
  },
  { title: '内容', key: 'content', minWidth: 200, ellipsis: { tooltip: true } },
  { title: 'IP', key: 'ip', width: 120 },
  { title: '归属地', key: 'ipLocation', width: 90, ellipsis: { tooltip: true } },
  {
    title: '状态',
    key: 'status',
    width: 90,
    render: (row) => {
      const s = statusMap[row.status] || { label: '-', type: 'default' }
      return h(NTag, { type: s.type, bordered: false }, { default: () => s.label })
    }
  },
  { title: '时间', key: 'createTime', width: 170 },
  {
    title: '操作',
    key: 'actions',
    align: 'right',
    width: 170,
    fixed: 'right',
    render: (row) =>
      hActionGroup(
        [
          // 只对已通过的评论提供回复：后端同样校验，待审/已拒绝的行先走审核流程
          userStore.hasPerm('blog:comment:reply') &&
            row.status === 1 &&
            hActionBtn({
              tip: replyTip.value,
              type: 'primary',
              icon: ArrowUndoOutline,
              disabled: busy(row.id) || replyDisabled.value,
              onClick: () => handleReply(row)
            }),
          userStore.hasPerm('blog:comment:audit') &&
            row.status !== 1 &&
            hActionBtn({
              tip: '通过',
              type: 'success',
              icon: CheckmarkOutline,
              disabled: busy(row.id),
              onClick: () => run(row.id, 'audit', () => handleAudit(row, 1))
            }),
          userStore.hasPerm('blog:comment:audit') &&
            row.status !== 2 &&
            hActionBtn({
              tip: '拒绝',
              type: 'warning',
              icon: CloseOutline,
              disabled: busy(row.id),
              onClick: () => run(row.id, 'audit', () => handleAudit(row, 2))
            }),
          userStore.hasPerm('blog:comment:remove') &&
            hActionConfirm({
              tip: '删除',
              icon: TrashOutline,
              confirm: '确定删除该评论吗？',
              loading: locked(row.id, 'delete'),
              disabled: busy(row.id),
              onPositiveClick: () => run(row.id, 'delete', () => handleDelete(row))
            })
        ].filter(Boolean)
      )
  }
])

async function handleAudit(row, status) {
  await auditComment({ id: row.id, status })
  message.success(status === 1 ? '已通过' : '已拒绝')
  loadData()
}

async function handleDelete(row) {
  await deleteComments([row.id])
  message.success('删除成功')
  loadData()
}

/** 拉取站点评论总开关与站长署名；失败时按默认处理，不阻塞列表（后端仍会兜底校验） */
async function loadCommentSwitch() {
  try {
    const res = await getSetting()
    commentEnabled.value = res.data?.commentEnabled != null ? res.data.commentEnabled : 1
    ownerNickname.value = res.data?.ownerNickname || '博主'
  } catch {
    commentEnabled.value = 1
  }
}

onMounted(loadCommentSwitch)
</script>

<style scoped>
.reply-quote {
  background: #f8f8f8;
  border-left: 3px solid var(--color-border, #e5e5e5);
  border-radius: 4px;
  padding: 10px 12px;
  margin-bottom: 16px;
  font-size: 13px;
}

.reply-quote__label {
  color: #999;
}

.reply-quote__name {
  color: var(--color-primary, #2080f0);
  font-weight: 600;
  margin-left: 4px;
}

.reply-quote__content {
  margin: 6px 0 0;
  color: #666;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 120px;
  overflow: auto;
}

/* 署名只读展示：不可编辑，视觉上区别于输入框 */
.reply-owner {
  color: var(--color-text-2, #666);
}
</style>
