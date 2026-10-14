<template>
  <div v-if="items.length" class="my-comments">
    <div class="my-comments__head">
      <h4>我的评论（审核中）</h4>
      <span class="my-comments__hint">通过审核后会出现在下方评论列表</span>
    </div>
    <ul class="my-comments__list">
      <li v-for="item in items" :key="item.id" class="my-comment">
        <div class="my-comment__body">
          <span v-if="item.parentId" class="my-comment__reply">回复</span>
          <span class="my-comment__content">{{ item.content }}</span>
        </div>
        <div class="my-comment__foot">
          <span class="my-comment__time">{{ item.createTime }}</span>
          <button
            type="button"
            class="my-comment__del"
            :disabled="deletingId === item.id"
            @click="handleDelete(item)"
          >
            {{ deletingId === item.id ? '删除中…' : '删除' }}
          </button>
        </div>
      </li>
    </ul>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { useDialog, useMessage } from 'naive-ui'
import { deleteMyComment, fetchMyComments } from '@/api/blog'
import { getCommenterKey } from '@/utils/commenterKey'

const props = defineProps({
  postId: { type: [Number, String], required: true },
  /** 提交成功后由父组件递增，用来触发刷新 */
  refreshToken: { type: Number, default: 0 }
})

const message = useMessage()
const dialog = useDialog()

/** 自己的待审评论；为空时整块不渲染 */
const items = ref([])
const deletingId = ref(null)

async function load() {
  if (!props.postId) return
  try {
    const res = await fetchMyComments(props.postId, getCommenterKey())
    items.value = res.data || []
  } catch {
    // 拿不到就当没有，不打扰阅读
    items.value = []
  }
}

function handleDelete(item) {
  dialog.warning({
    title: '删除评论',
    content: '确定删除这条评论吗？删除后无法恢复。',
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      deletingId.value = item.id
      try {
        await deleteMyComment(item.id, getCommenterKey())
        message.success('已删除')
        await load()
      } finally {
        deletingId.value = null
      }
    }
  })
}

watch(() => props.refreshToken, load)
onMounted(load)

defineExpose({ load })
</script>

<style scoped>
.my-comments {
  margin-top: 24px;
  padding: 16px 18px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: #fafafa;
}

.my-comments__head {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 12px;
}

.my-comments__head h4 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
}

.my-comments__hint {
  font-size: 12px;
  color: var(--color-text-muted);
}

.my-comments__list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.my-comment {
  padding: 12px 0;
  border-top: 1px solid var(--color-border);
}

.my-comment__body {
  font-size: 14px;
  line-height: 1.7;
  color: #444;
  white-space: pre-wrap;
  word-break: break-word;
}

.my-comment__reply {
  display: inline-block;
  margin-right: 6px;
  padding: 0 6px;
  border-radius: 4px;
  background: rgba(0, 0, 0, 0.06);
  font-size: 12px;
  color: var(--color-text-secondary);
}

.my-comment__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 8px;
}

.my-comment__time {
  font-size: 12px;
  color: var(--color-text-muted);
}

.my-comment__del {
  border: none;
  background: none;
  padding: 0;
  color: #d03050;
  font-size: 13px;
  font-family: inherit;
  cursor: pointer;
}

.my-comment__del:disabled {
  color: var(--color-text-muted);
  cursor: default;
}
</style>
