<template>
  <div class="page-container">
    <SearchBar :pager="pager">
      <template #fields>
        <n-form-item label="登录账号">
          <n-input v-model:value="query.username" placeholder="登录账号" clearable />
        </n-form-item>
        <n-form-item label="状态">
          <n-select v-model:value="query.status" :options="statusOptions" placeholder="全部" clearable />
        </n-form-item>
      </template>
    </SearchBar>

    <PageTable :pager="pager" :columns="columns" />
  </div>
</template>

<script setup>
import { h } from 'vue'
import { NTag } from 'naive-ui'
import { pageLoginLogs } from '@/modules/system/api/system'
import { usePagination } from '@/composables/usePagination'

const pager = usePagination((q) => pageLoginLogs(q), {
  defaults: {
    username: '',
    status: null
  }
})
const { query } = pager

const statusOptions = [
  { label: '成功', value: 1 },
  { label: '失败', value: 0 }
]

const columns = [
  { title: 'ID', key: 'id', width: 70 },
  { title: '登录账号', key: 'username', width: 130 },
  { title: '登录 IP', key: 'ip', width: 140 },
  { title: '浏览器', key: 'browser', width: 160, ellipsis: { tooltip: true } },
  { title: '操作系统', key: 'os', width: 150, ellipsis: { tooltip: true } },
  {
    title: '状态',
    key: 'status',
    width: 100,
    render: (row) =>
      h(
        NTag,
        { type: row.status === 1 ? 'success' : 'error', bordered: false },
        { default: () => (row.status === 1 ? '成功' : '失败') }
      )
  },
  { title: '提示信息', key: 'msg', width: 160, ellipsis: { tooltip: true } },
  { title: '登录时间', key: 'loginTime', width: 190 }
]
</script>
