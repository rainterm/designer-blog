<template>
  <article class="post-card" @click="goDetail">
    <div v-if="coverSrc" class="post-card__cover">
      <img :src="coverSrc" :alt="post.title" loading="lazy" />
    </div>
    <div class="post-card__body">
      <h2 class="post-card__title">{{ post.title }}</h2>
      <p v-if="post.summary" class="post-card__summary">{{ post.summary }}</p>
      <div class="post-card__meta">
        <span v-if="post.categoryName" class="post-card__cat">{{ post.categoryName }}</span>
        <span v-if="post.publishedAt">{{ formatDate(post.publishedAt) }}</span>
        <span v-if="post.viewCount != null">{{ formatViews(post.viewCount) }} 阅读</span>
      </div>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { formatDate, formatViews } from '@/utils/format'

const props = defineProps({
  post: { type: Object, required: true }
})

const router = useRouter()
const coverSrc = computed(() => props.post.coverUrl || '')

function goDetail() {
  if (!props.post.slug) return
  router.push({ name: 'post', params: { slug: props.post.slug } })
}
</script>

<style scoped>
.post-card {
  display: flex;
  gap: 16px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 16px;
  cursor: pointer;
  transition: border-color 0.15s, box-shadow 0.15s, transform 0.15s;
}

.post-card:hover {
  border-color: color-mix(in srgb, var(--color-primary) 28%, var(--color-border));
  box-shadow: 0 8px 20px -10px rgba(0, 0, 0, 0.1);
  transform: translateY(-1px);
}

.post-card:hover .post-card__title {
  color: var(--color-primary);
}

.post-card__cover {
  flex-shrink: 0;
  width: 120px;
  height: 90px;
  border-radius: var(--radius-md);
  overflow: hidden;
  background: var(--color-body);
}

.post-card__cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.post-card__body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  padding: 2px 4px 2px 0;
}

.post-card__title {
  font-family: var(--font-display);
  font-size: 18px;
  font-weight: 600;
  line-height: 1.4;
  letter-spacing: -0.01em;
  margin-bottom: 8px;
  color: var(--color-text);
  transition: color 0.15s;
}

.post-card__summary {
  font-size: 14px;
  color: var(--color-text-secondary);
  line-height: 1.65;
  margin-bottom: 14px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.post-card__meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 14px;
  margin-top: auto;
  font-size: 12px;
  color: var(--color-text-muted);
}

.post-card__cat {
  color: var(--color-primary);
  font-weight: 500;
}

@media (max-width: 480px) {
  .post-card__cover {
    width: 88px;
    height: 72px;
  }

  .post-card__title {
    font-size: 16px;
  }
}
</style>
