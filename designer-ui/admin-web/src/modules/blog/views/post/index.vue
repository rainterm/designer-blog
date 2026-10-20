<template>
  <div class="page-container">
    <SearchBar :pager="pager">
      <template #fields>
        <n-form-item label="关键词">
          <n-input v-model:value="query.keyword" placeholder="标题 / 摘要" clearable />
        </n-form-item>
        <n-form-item label="分类">
          <n-select
            v-model:value="query.categoryId"
            :options="categoryOptions"
            placeholder="全部"
            clearable
          />
        </n-form-item>
        <n-form-item label="状态">
          <n-select v-model:value="query.status" :options="statusOptions" placeholder="全部" clearable />
        </n-form-item>
      </template>
    </SearchBar>

    <PageTable :pager="pager" :columns="columns">
      <template #toolbar-right>
        <n-space>
          <n-button v-perm="'blog:post:add'" type="primary" @click="goEdit()">
            写文章
          </n-button>
          <n-button
            v-perm="'blog:post:remove'"
            type="error"
            :disabled="!checkedRowKeys.length"
            @click="handleBatchDelete"
          >
            批量删除
          </n-button>
        </n-space>
      </template>
    </PageTable>
  </div>
</template>

<script setup>
import { computed, h, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { NTag, useMessage } from 'naive-ui'
import { CreateOutline, EyeOutline, EyeOffOutline, TrashOutline } from '@vicons/ionicons5'
import { hActionBtn, hActionConfirm, hActionGroup } from '@/utils/actionBtn'
import { deletePosts, listCategories, pagePosts, publishPost } from '@/modules/blog/api'
import { useActionLock } from '@/composables/useActionLock'
import { usePagination } from '@/composables/usePagination'
import { useBatchDelete } from '@/composables/useBatchDelete'
import { useUserStore } from '@/store/user'

const router = useRouter()
const message = useMessage()
const userStore = useUserStore()
const { locked, busy, run } = useActionLock()

const categoryOptions = ref([])

const pager = usePagination((q) => pagePosts(q), {
  defaults: {
    keyword: '',
    categoryId: null,
    status: null
  }
})
const { query, loadData, checkedRowKeys } = pager
const { handleBatchDelete } = useBatchDelete(pager, deletePosts, {
  confirm: (n) => `确定删除选中的 ${n} 篇文章吗？相关评论将一并删除。`,
  positiveText: '删除'
})

const statusOptions = [
  { label: '草稿', value: 0 },
  { label: '已发布', value: 1 }
]

const columns = computed(() => [
  { type: 'selection', width: 44 },
  { title: 'ID', key: 'id', width: 70 },
  {
    title: '标题',
    key: 'title',
    with: 200,
    ellipsis: { tooltip: true },
    render: (row) =>
      h('div', { style: 'display:flex;align-items:center;gap:6px' }, [
        row.isTop === 1
          ? h(NTag, { size: 'small', type: 'warning', bordered: false }, { default: () => '置顶' })
          : null,
        h('span', row.title)
      ])
  },
  { title: '分类', key: 'categoryName', width: 100 },
  { title: '作者', key: 'authorName', width: 100 },
  {
    title: '状态',
    key: 'status',
    width: 90,
    render: (row) =>
      h(NTag, { type: row.status === 1 ? 'success' : 'default', bordered: false }, {
        default: () => (row.status === 1 ? '已发布' : '草稿')
      })
  },
  { title: '浏览', key: 'viewCount', width: 80 },
  { title: '发布时间', key: 'publishedAt', width: 180 },
  {
    title: '更新时间',
    key: 'updateTime',
    width: 180,
    // 新建后未改过的文章两者一致，直接展示即可；缺值时用占位符避免列塌陷
    render: (row) => row.updateTime || '-'
  },
  {
    title: '操作',
    key: 'actions',
    align: 'right',
    width: 150,
    fixed: 'right',
    render: (row) =>
      hActionGroup(
        [
          userStore.hasPerm('blog:post:edit') &&
            hActionBtn({
              tip: '编辑',
              type: 'primary',
              icon: CreateOutline,
              disabled: busy(row.id),
              onClick: () => goEdit(row.id)
            }),
          userStore.hasPerm('blog:post:publish') &&
            hActionBtn({
              tip: row.status === 1 ? '下架' : '发布',
              type: row.status === 1 ? 'warning' : 'success',
              icon: row.status === 1 ? EyeOffOutline : EyeOutline,
              disabled: busy(row.id),
              onClick: () =>
                run(row.id, 'publish', () => handlePublish(row, row.status !== 1))
            }),
          userStore.hasPerm('blog:post:remove') &&
            hActionConfirm({
              tip: '删除',
              icon: TrashOutline,
              confirm: `确定删除文章「${row.title}」吗？`,
              loading: locked(row.id, 'delete'),
              disabled: busy(row.id),
              onPositiveClick: () => run(row.id, 'delete', () => handleDelete(row))
            })
        ].filter(Boolean)
      )
  }
])

async function loadCategories() {
  const res = await listCategories()
  categoryOptions.value = (res.data || []).map((c) => ({ label: c.name, value: c.id }))
}

function goEdit(id) {
  // 编辑为独立全屏页（无侧栏/顶栏），直接在新标签打开，避免丢失列表页的查询条件
  const { href } = router.resolve({ path: '/blog/post/edit', query: id ? { id } : {} })
  window.open(href, '_blank', 'noopener')
}

async function handlePublish(row, publish) {
  await publishPost(row.id, publish)
  message.success(publish ? '已发布' : '已下架')
  loadData()
}

async function handleDelete(row) {
  await deletePosts([row.id])
  message.success('删除成功')
  loadData()
}

onMounted(() => {
  loadCategories()
})
</script>
