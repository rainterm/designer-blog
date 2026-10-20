<template>
  <div class="page-container">
    <SearchBar :pager="pager">
      <template #fields>
        <n-form-item label="关键词">
          <n-input v-model:value="query.keyword" placeholder="路径 / IP / 归属地" clearable />
        </n-form-item>
        <n-form-item label="页面类型">
          <n-select v-model:value="query.pageType" :options="pageTypeOptions" placeholder="全部" clearable />
        </n-form-item>
        <n-form-item label="设备">
          <n-select v-model:value="query.device" :options="deviceOptions" placeholder="全部" clearable />
        </n-form-item>
        <n-form-item label="来源">
          <n-select
            v-model:value="query.refererType"
            :options="refererTypeOptions"
            placeholder="全部"
            clearable
          />
        </n-form-item>
        <n-form-item label="访问日期">
          <!-- 用 formatted-value 直接拿 yyyy-MM-dd，避免时间戳再手动转一次 -->
          <n-date-picker
            v-model:formatted-value="dateRange"
            type="daterange"
            value-format="yyyy-MM-dd"
            clearable
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            class="date-range"
          />
        </n-form-item>
      </template>
    </SearchBar>

    <PageTable :pager="pager" :columns="columns" />
  </div>
</template>

<script setup>
import { computed, h } from 'vue'
import { NTag } from 'naive-ui'
import { pageVisitLogs } from '@/modules/blog/api'
import { usePagination } from '@/composables/usePagination'
import { useDict } from '@/composables/useDict'
import {
  DICT_BLOG_VISIT_DEVICE,
  DICT_BLOG_VISIT_PAGE_TYPE,
  DICT_BLOG_VISIT_REFERER_TYPE
} from '@/constants/dict'

const pager = usePagination((q) => pageVisitLogs(q), {
  defaults: {
    keyword: '',
    pageType: null,
    device: null,
    refererType: null,
    beginDate: '',
    endDate: ''
  }
})
const { query } = pager

/** 日期选择器与查询条件双向绑定：清空后两个字段一起置空，避免半截条件发到后端 */
const dateRange = computed({
  get: () => (query.beginDate && query.endDate ? [query.beginDate, query.endDate] : null),
  set: (range) => {
    query.beginDate = range?.[0] || ''
    query.endDate = range?.[1] || ''
  }
})

/** 筛选项全部来自数据字典：与列表返回的 *Label 同源，改名只需在「系统管理 → 数据字典」改一次 */
const { options: pageTypeOptions } = useDict(DICT_BLOG_VISIT_PAGE_TYPE)
const { options: deviceOptions } = useDict(DICT_BLOG_VISIT_DEVICE)
const { options: refererTypeOptions } = useDict(DICT_BLOG_VISIT_REFERER_TYPE)

/** 页面类型 / 设备只决定标签颜色，文案以服务端按字典转换后的 label 为准 */
const pageTypeColors = {
  home: 'info',
  post: 'success',
  tag: 'warning',
  category: 'primary',
  other: 'default'
}

const deviceColors = {
  PC: 'info',
  MOBILE: 'success',
  TABLET: 'warning',
  BOT: 'error',
  OTHER: 'default'
}

const columns = [
  { title: 'ID', key: 'id', width: 80 },
  { title: '访问时间', key: 'createTime', width: 180 },
  {
    title: '页面类型',
    key: 'pageTypeLabel',
    width: 110,
    render: (row) =>
      h(
        NTag,
        { type: pageTypeColors[row.pageType] || 'default', bordered: false },
        { default: () => row.pageTypeLabel || '-' }
      )
  },
  { title: '访问路径', key: 'path', width: 240, ellipsis: { tooltip: true } },
  { title: 'IP', key: 'ip', width: 130 },
  { title: '归属地', key: 'ipLocation', width: 100, ellipsis: { tooltip: true } },
  {
    title: '设备',
    key: 'deviceLabel',
    width: 100,
    render: (row) =>
      h(
        NTag,
        { type: deviceColors[row.device] || 'default', bordered: false },
        { default: () => row.deviceLabel || '-' }
      )
  },
  {
    title: '来源',
    key: 'refererHost',
    width: 210,
    render: (row) => {
      if (!row.refererHost) {
        // 空域名一定是直接访问，文案同样取字典
        return h('span', { class: 'referer-direct' }, row.refererTypeLabel || '-')
      }
      return h('div', { style: 'display:flex;align-items:center;gap:6px' }, [
        h('span', { style: 'overflow:hidden;text-overflow:ellipsis;white-space:nowrap' }, row.refererHost),
        h(
          NTag,
          { size: 'small', bordered: false, type: row.refererType === 'search' ? 'warning' : 'info' },
          { default: () => row.refererTypeLabel || row.refererType || '-' }
        )
      ])
    }
  },
  { title: 'User-Agent', key: 'ua', width: 260, ellipsis: { tooltip: true } }
]
</script>

<style scoped>
.date-range {
  width: 240px;
}

.referer-direct {
  color: var(--color-text-muted);
}
</style>
