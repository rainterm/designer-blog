<template>
  <div class="blog-page fade-in">
    <nav v-if="categories.length" class="cat-nav" aria-label="分类">
      <button
        type="button"
        class="cat-nav__item"
        :class="{ active: !activeCategory && !activeTag }"
        @click="clearFilter"
      >
        全部
      </button>
      <button
        v-for="c in categories"
        :key="c.id"
        type="button"
        class="cat-nav__item"
        :class="{ active: activeCategory === c.slug }"
        @click="filterCategory(c.slug)"
      >
        {{ c.name }}
      </button>
    </nav>

    <n-spin :show="loading">
      <div v-if="posts.length" class="post-list">
        <PostCard v-for="p in posts" :key="p.id" :post="p" />
      </div>
      <n-empty v-else-if="!loading" :description="blogPage.emptyText" style="margin-top: 56px" />
    </n-spin>

    <div v-if="total > pageSize" class="pager">
      <PaginationBar
        :page="pageNum"
        :page-size="pageSize"
        :item-count="total"
        :show-size-picker="false"
        :show-quick-jumper="false"
        @update:page="reload"
      />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PaginationBar from '@/components/PaginationBar.vue'
import PostCard from '@/components/PostCard.vue'
import { fetchCategories, fetchPosts } from '@/api/blog'
import { blogPage } from '@/config/site'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const posts = ref([])
const categories = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = blogPage.pageSize || 10

const activeCategory = computed(() =>
  route.name === 'category' ? String(route.params.slug || '') : ''
)
const activeTag = computed(() =>
  route.name === 'tag' ? String(route.params.slug || '') : ''
)
const keyword = computed(() =>
  typeof route.query.q === 'string' ? route.query.q.trim() : ''
)

async function loadMeta() {
  try {
    const cRes = await fetchCategories()
    categories.value = cRes.data || []
  } catch {
    categories.value = []
  }
}

async function reload(page = pageNum.value) {
  pageNum.value = page
  loading.value = true
  try {
    const params = {
      pageNum: pageNum.value,
      pageSize,
      keyword: keyword.value || undefined
    }
    if (activeCategory.value) params.categorySlug = activeCategory.value
    if (activeTag.value) params.tagSlug = activeTag.value
    const res = await fetchPosts(params)
    posts.value = res.data?.records || []
    total.value = Number(res.data?.total) || 0
  } catch {
    posts.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function clearFilter() {
  router.push({ name: 'blog', query: keyword.value ? { q: keyword.value } : {} })
}

function filterCategory(slug) {
  router.push({
    name: 'category',
    params: { slug },
    query: keyword.value ? { q: keyword.value } : {}
  })
}

watch(
  () => [route.name, route.params.slug, route.query.q],
  () => reload(1)
)

onMounted(async () => {
  await loadMeta()
  await reload(1)
})
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
