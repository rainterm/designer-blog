<template>
  <div class="page-container">
    <SearchBar :pager="pager">
      <template #fields>
        <n-form-item label="名称">
          <n-input v-model:value="query.name" placeholder="链接名称" clearable />
        </n-form-item>
        <n-form-item label="状态">
          <n-select v-model:value="query.status" :options="statusOptions" placeholder="全部" clearable />
        </n-form-item>
      </template>
    </SearchBar>

    <PageTable :pager="pager" :columns="columns">
      <template #toolbar-right>
        <n-space>
          <n-button v-perm="'blog:link:add'" type="primary" @click="openCreate">
            新增链接
          </n-button>
          <n-button
            v-perm="'blog:link:remove'"
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
      v-model:show="formVisible"
      :title="isEdit ? '编辑链接' : '新增链接'"
      :loading="submitting"
      @confirm="handleSubmit"
    >
      <n-form ref="formRef" :model="form" :rules="formRules" label-placement="left" label-width="80">
        <n-form-item label="名称" path="name">
          <n-input v-model:value="form.name" placeholder="如 GitHub" />
        </n-form-item>
        <n-form-item label="地址" path="url">
          <n-input v-model:value="form.url" placeholder="https://github.com/yourname" />
        </n-form-item>
        <n-form-item label="图标">
          <n-select
            v-model:value="form.icon"
            :options="iconOptions"
            :render-label="renderIconLabel"
            placeholder="可选，支持输入自定义标识"
            filterable
            tag
            clearable
          />
        </n-form-item>
        <n-form-item label="排序" path="sort">
          <n-input-number v-model:value="form.sort" :min="0" style="width: 100%" />
        </n-form-item>
        <n-form-item label="状态" path="status">
          <n-radio-group v-model:value="form.status">
            <n-radio :value="1">启用</n-radio>
            <n-radio :value="0">停用</n-radio>
          </n-radio-group>
        </n-form-item>
        <n-form-item label="备注">
          <n-input v-model:value="form.remark" type="textarea" :rows="2" placeholder="选填" />
        </n-form-item>
      </n-form>
    </Dialog>
  </div>
</template>

<script setup>
import { computed, h } from 'vue'
import { NTag, useMessage } from 'naive-ui'
import { CreateOutline, OpenOutline, TrashOutline } from '@vicons/ionicons5'
import { hActionBtn, hActionConfirm, hActionGroup } from '@/utils/actionBtn'
import LinkIcon from '@/components/LinkIcon.vue'
import { createLink, deleteLinks, pageLinks, updateLink } from '@/modules/blog/api'
import { useActionLock } from '@/composables/useActionLock'
import { usePagination } from '@/composables/usePagination'
import { useBatchDelete } from '@/composables/useBatchDelete'
import { useFormDialog } from '@/composables/useFormDialog'
import { useUserStore } from '@/store/user'

const message = useMessage()
const userStore = useUserStore()
const { locked, busy, run } = useActionLock()

const pager = usePagination((q) => pageLinks(q), {
  defaults: { name: '', status: null }
})
const { query, loadData, checkedRowKeys } = pager
const { handleBatchDelete } = useBatchDelete(pager, deleteLinks, {
  confirm: (n) => `确定删除选中的 ${n} 个链接吗？`,
  positiveText: '删除'
})

const {
  visible: formVisible,
  submitting,
  formRef,
  form,
  isEdit,
  openCreate,
  openEdit,
  submit: handleSubmit
} = useFormDialog({
  defaults: { id: null, name: '', url: '', icon: '', sort: 0, status: 1, remark: '' },
  async onSubmit(payload) {
    if (payload.id) {
      await updateLink({ ...payload })
      message.success('更新成功')
    } else {
      await createLink({ ...payload })
      message.success('新增成功')
    }
    loadData()
  }
})

const statusOptions = [
  { label: '启用', value: 1 },
  { label: '停用', value: 0 }
]

/** 内置图标标识（前台按标识匹配内置 SVG）；也可输入自定义标识，未知标识回退为通用链接图标 */
const iconOptions = [
  { label: 'github', value: 'github' },
  { label: 'gitee', value: 'gitee' },
  { label: 'link（通用链接）', value: 'link' },
  { label: 'globe（网站）', value: 'globe' },
  { label: 'mail（邮箱）', value: 'mail' }
]

/** 图标选择项预览：图标 + 文案（render-label 同时作用于下拉选项与已选值） */
function renderIconLabel(option) {
  return h('span', { style: 'display:inline-flex;align-items:center;gap:6px;' }, [
    h(LinkIcon, { name: option.value, size: 16 }),
    h('span', null, option.label)
  ])
}

const formRules = {
  name: { required: true, message: '请输入名称', trigger: 'blur' },
  url: [
    { required: true, message: '请输入地址', trigger: 'blur' },
    {
      validator: (_rule, value) =>
        /^https?:\/\//i.test(String(value || '').trim()),
      message: '地址需以 http:// 或 https:// 开头',
      trigger: 'blur'
    }
  ]
}

function openUrl(url) {
  if (url) window.open(url, '_blank', 'noopener,noreferrer')
}

const columns = computed(() => [
  { type: 'selection', width: 44 },
  { title: 'ID', key: 'id', width: 70 },
  { title: '名称', key: 'name', width: 140 },
  {
    title: '地址',
    key: 'url',
    minWidth: 220,
    ellipsis: { tooltip: true },
    render: (row) =>
      h(
        'a',
        {
          href: row.url,
          target: '_blank',
          rel: 'noopener noreferrer',
          style: 'color: var(--color-primary)'
        },
        row.url
      )
  },
  {
    title: '图标',
    key: 'icon',
    width: 110,
    render: (row) =>
      row.icon
        ? h('span', { style: 'display:inline-flex;align-items:center;gap:6px;' }, [
            h(LinkIcon, { name: row.icon, size: 16 }),
            h('span', null, row.icon)
          ])
        : '-'
  },
  { title: '排序', key: 'sort', width: 80 },
  {
    title: '状态',
    key: 'status',
    width: 90,
    render: (row) =>
      h(NTag, { type: row.status === 1 ? 'success' : 'default', bordered: false }, {
        default: () => (row.status === 1 ? '启用' : '停用')
      })
  },
  { title: '备注', key: 'remark', width: 140, ellipsis: { tooltip: true } },
  {
    title: '操作',
    key: 'actions',
    align: 'right',
    width: 140,
    fixed: 'right',
    render: (row) =>
      hActionGroup(
        [
          hActionBtn({
            tip: '访问',
            type: 'info',
            icon: OpenOutline,
            disabled: busy(row.id),
            onClick: () => openUrl(row.url)
          }),
          userStore.hasPerm('blog:link:edit') &&
            hActionBtn({
              tip: '编辑',
              type: 'primary',
              icon: CreateOutline,
              disabled: busy(row.id),
              onClick: () =>
                openEdit(row, (r) => ({
                  id: r.id,
                  name: r.name,
                  url: r.url,
                  icon: r.icon || '',
                  sort: r.sort ?? 0,
                  status: r.status,
                  remark: r.remark || ''
                }))
            }),
          userStore.hasPerm('blog:link:remove') &&
            hActionConfirm({
              tip: '删除',
              icon: TrashOutline,
              confirm: `确定删除链接「${row.name}」吗？`,
              loading: locked(row.id, 'delete'),
              disabled: busy(row.id),
              onPositiveClick: () => run(row.id, 'delete', () => handleDelete(row))
            })
        ].filter(Boolean)
      )
  }
])

async function handleDelete(row) {
  await deleteLinks([row.id])
  message.success('删除成功')
  loadData()
}
</script>
