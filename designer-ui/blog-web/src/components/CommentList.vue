<template>
  <div class="comment-list">
    <div v-if="!flat.length" class="comment-empty">
      <p>暂无评论，来抢沙发吧</p>
    </div>
    <div v-for="item in flat" :key="item.id" class="comment-item" :style="{ marginLeft: item.depth * 24 + 'px' }">
      <div class="comment-item__head">
        <span class="comment-item__name">{{ item.nickname }}</span>
        <!-- 第 2 层统一压平显示，用「回复 某某」标出这句话是回给谁的 -->
        <span v-if="item.replyToNickname" class="comment-item__reply-to">
          回复 <span class="comment-item__reply-to-name">{{ item.replyToNickname }}</span>
        </span>
        <span class="comment-item__time">{{ item.createTime }}</span>
        <span v-if="item.ipLocation" class="comment-item__location">{{ item.ipLocation }}</span>
      </div>
      <p class="comment-item__content">{{ item.content }}</p>
      <button
        v-if="allowReply"
        type="button"
        class="comment-item__reply"
        @click="$emit('reply', item)"
      >
        回复
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  comments: { type: Array, default: () => [] },
  allowReply: { type: Boolean, default: true }
})

defineEmits(['reply'])

/** 把评论树摊平成带缩进层级的列表（迭代实现，避免深层回复链递归爆栈） */
const flat = computed(() => {
  const result = []
  const stack = (props.comments || []).map((c) => ({ node: c, depth: 0 })).reverse()
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
</script>

<style scoped>
.comment-empty {
  background: #f8f8f8;
  border-radius: var(--radius-lg);
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
  align-items: baseline;
  margin-bottom: 8px;
}

.comment-item__name {
  font-weight: 600;
  font-size: 14px;
}

.comment-item__time {
  font-size: 12px;
  color: var(--color-text-muted);
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

.comment-item__content {
  font-size: 14px;
  line-height: 1.7;
  color: #444;
  white-space: pre-wrap;
}

.comment-item__reply {
  margin-top: 8px;
  border: none;
  background: none;
  color: var(--color-primary);
  font-size: 13px;
  cursor: pointer;
  padding: 0;
}
</style>
