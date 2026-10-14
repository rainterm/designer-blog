<template>
  <div class="page-container visit-page">
    <n-spin :show="loading">
      <section class="visit-section">
        <div class="section-head">
          <div class="section-head-left">
            <h2>访问统计</h2>
            <span class="section-tip">数据来自前台埋点，按天聚合</span>
          </div>
          <div class="section-head-right">
            <n-radio-group v-model:value="days" size="small" @update:value="loadData">
              <n-radio-button v-for="opt in rangeOptions" :key="opt.value" :value="opt.value">
                {{ opt.label }}
              </n-radio-button>
            </n-radio-group>
            <n-button quaternary :loading="loading" @click="loadData">刷新</n-button>
          </div>
        </div>

        <!-- 概览数字 -->
        <div class="stat-row">
          <div v-for="card in summaryCards" :key="card.key" class="stat-card">
            <div class="stat-card__label">{{ card.label }}</div>
            <div class="stat-card__value" :style="{ color: card.color }">{{ card.value }}</div>
            <div class="stat-card__sub">{{ card.sub }}</div>
          </div>
        </div>

        <!-- PV / UV 趋势 -->
        <div class="panel">
          <div class="panel-head">
            <span class="panel-title">访问趋势</span>
            <span class="panel-tip">PV 为浏览量，UV 为按访客去重的独立访客数</span>
          </div>
          <div v-show="hasTrend" ref="trendEl" class="chart chart--tall" />
          <n-empty v-if="!hasTrend" size="small" description="暂无访问数据" class="chart-empty" />
        </div>

        <div class="chart-grid">
          <!-- 分类访问分布 -->
          <div class="panel">
            <div class="panel-head">
              <span class="panel-title">分类访问分布</span>
              <span class="panel-tip">仅统计文章详情页</span>
            </div>
            <div v-show="hasCategories" ref="categoryEl" class="chart" />
            <n-empty v-if="!hasCategories" size="small" description="暂无数据" class="chart-empty" />
          </div>

          <!-- 设备占比 -->
          <div class="panel">
            <div class="panel-head">
              <span class="panel-title">设备占比</span>
              <span class="panel-tip">按 User-Agent 归类</span>
            </div>
            <div v-show="hasDevices" ref="deviceEl" class="chart" />
            <n-empty v-if="!hasDevices" size="small" description="暂无数据" class="chart-empty" />
          </div>

          <!-- 来源 Top -->
          <div class="panel">
            <div class="panel-head">
              <span class="panel-title">来源 Top</span>
              <span class="panel-tip">站内跳转不计入来源</span>
            </div>
            <div v-show="hasReferers" ref="refererEl" class="chart" />
            <n-empty v-if="!hasReferers" size="small" description="暂无数据" class="chart-empty" />
          </div>

          <!-- 地域分布 -->
          <div class="panel">
            <div class="panel-head">
              <span class="panel-title">访客地域分布</span>
              <span class="panel-tip">Top 10 省份</span>
            </div>
            <div v-show="hasRegions" ref="regionEl" class="chart" />
            <n-empty v-if="!hasRegions" size="small" description="暂无数据" class="chart-empty" />
          </div>
        </div>
      </section>
    </n-spin>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts/core'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { getVisitOverview } from '@/modules/blog/api'
import { colors } from '@/theme'

echarts.use([LineChart, PieChart, BarChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

/** 多系列配色：首色跟随主题主色，其余为对比度足够的固定色 */
const SERIES_COLORS = [
  readCssVar('--color-primary') || colors.primary,
  '#0EA5E9',
  '#F59E0B',
  '#8B5CF6',
  '#EF4444',
  '#14B8A6',
  '#6366F1',
  '#EC4899'
]

const rangeOptions = [
  { label: '近 7 天', value: 7 },
  { label: '近 30 天', value: 30 },
  { label: '近 90 天', value: 90 }
]

const loading = ref(false)
const days = ref(30)
const overview = ref(null)

const trendEl = ref(null)
const categoryEl = ref(null)
const deviceEl = ref(null)
const refererEl = ref(null)
const regionEl = ref(null)

let charts = {}

const summary = computed(() => overview.value?.summary || {})
const trend = computed(() => overview.value?.trend || [])
const categories = computed(() => overview.value?.categories || [])
const referers = computed(() => overview.value?.referers || [])
const devices = computed(() => overview.value?.devices || [])
const regions = computed(() => overview.value?.regions || [])

const hasTrend = computed(() => trend.value.some((d) => d.pv > 0 || d.uv > 0))
const hasCategories = computed(() => categories.value.length > 0)
const hasDevices = computed(() => devices.value.length > 0)
const hasReferers = computed(() => referers.value.length > 0)
const hasRegions = computed(() => regions.value.length > 0)

const summaryCards = computed(() => [
  { key: 'pv', label: '浏览量 PV', value: fmt(summary.value.pv), sub: `近 ${overview.value?.days ?? days.value} 天`, color: SERIES_COLORS[0] },
  { key: 'uv', label: '独立访客 UV', value: fmt(summary.value.uv), sub: '按访客去重', color: SERIES_COLORS[1] },
  { key: 'todayPv', label: '今日 PV', value: fmt(summary.value.todayPv), sub: '今天 00:00 起', color: SERIES_COLORS[2] },
  { key: 'todayUv', label: '今日 UV', value: fmt(summary.value.todayUv), sub: '今天 00:00 起', color: SERIES_COLORS[3] }
])

function readCssVar(name) {
  if (typeof window === 'undefined') return ''
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim()
}

function fmt(v) {
  const n = Number(v)
  return Number.isFinite(n) ? n.toLocaleString('zh-CN') : '0'
}

/** 通用坐标轴样式，避免每个图重复写 */
function axisStyle() {
  return {
    axisLine: { lineStyle: { color: colors.border } },
    axisTick: { show: false },
    axisLabel: { color: colors.textMuted, fontSize: 11 }
  }
}

function trendOption() {
  const dates = trend.value.map((d) => d.date)
  return {
    color: SERIES_COLORS,
    tooltip: { trigger: 'axis' },
    legend: { data: ['PV', 'UV'], right: 0, top: 0, icon: 'roundRect', textStyle: { color: colors.textSecondary } },
    grid: { left: 8, right: 12, top: 36, bottom: 4, containLabel: true },
    xAxis: { type: 'category', boundaryGap: false, data: dates, ...axisStyle() },
    yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: colors.border } }, ...axisStyle() },
    series: [
      {
        name: 'PV',
        type: 'line',
        smooth: true,
        showSymbol: false,
        data: trend.value.map((d) => d.pv),
        areaStyle: { opacity: 0.12 }
      },
      {
        name: 'UV',
        type: 'line',
        smooth: true,
        showSymbol: false,
        data: trend.value.map((d) => d.uv)
      }
    ]
  }
}

/** 环形图配置：分类与设备共用 */
function pieOption(rows) {
  return {
    color: SERIES_COLORS,
    tooltip: { trigger: 'item', formatter: '{b}<br/>{c} 次（{d}%）' },
    legend: { orient: 'vertical', right: 0, top: 'middle', icon: 'roundRect', textStyle: { color: colors.textSecondary, fontSize: 12 } },
    series: [
      {
        type: 'pie',
        radius: ['45%', '68%'],
        center: ['36%', '50%'],
        avoidLabelOverlap: true,
        itemStyle: { borderColor: colors.surface, borderWidth: 2 },
        label: { show: false },
        labelLine: { show: false },
        data: rows.map((r) => ({ name: r.name, value: r.value }))
      }
    ]
  }
}

/** 横向条形图配置：来源与地域共用；tooltipFormatter 用于补充维度信息 */
function barOption(rows, colorIndex = 0, tooltipFormatter) {
  // 倒序：让数量最多的排在最上方（yAxis 自下而上排列）
  const sorted = [...rows].reverse()
  return {
    tooltip: tooltipFormatter
      ? { trigger: 'axis', axisPointer: { type: 'shadow' }, formatter: tooltipFormatter }
      : { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 8, right: 28, top: 8, bottom: 4, containLabel: true },
    xAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: colors.border } }, ...axisStyle() },
    yAxis: {
      type: 'category',
      data: sorted.map((r) => r.name),
      ...axisStyle(),
      axisLabel: { color: colors.textSecondary, fontSize: 12, width: 120, overflow: 'truncate' }
    },
    series: [
      {
        type: 'bar',
        barMaxWidth: 18,
        itemStyle: { color: SERIES_COLORS[colorIndex], borderRadius: [0, 4, 4, 0] },
        label: { show: true, position: 'right', color: colors.textMuted, fontSize: 11 },
        data: sorted.map((r) => r.value)
      }
    ]
  }
}

/** 来源域名：空域名代表直接访问，来源类型一并带出便于判断 */
function refererRows() {
  const typeLabel = { direct: '直接访问', search: '搜索引擎', external: '外链' }
  return referers.value.map((r) => ({
    name: r.host || '(直接访问)',
    value: r.value,
    type: typeLabel[r.type] || ''
  }))
}

/** 来源图的 tooltip：把来源类型补在域名后面 */
function refererTooltip(params) {
  const p = Array.isArray(params) ? params[0] : params
  const row = refererRows().find((r) => r.name === p.name)
  const suffix = row?.type ? `（${row.type}）` : ''
  return `${p.name}${suffix}<br/>${p.value} 次`
}

async function loadData() {
  loading.value = true
  try {
    const res = await getVisitOverview({ days: days.value })
    overview.value = res.data || null
    await nextTick()
    renderCharts()
  } catch {
    // 拦截器已统一提示
  } finally {
    loading.value = false
  }
}

function renderCharts() {
  // 容器在 v-show 隐藏时尺寸为 0，只对可见图表初始化
  if (trendEl.value && hasTrend.value) {
    charts.trend = charts.trend || echarts.init(trendEl.value)
    charts.trend.setOption(trendOption(), true)
  }
  if (categoryEl.value && hasCategories.value) {
    charts.category = charts.category || echarts.init(categoryEl.value)
    charts.category.setOption(pieOption(categories.value), true)
  }
  if (deviceEl.value && hasDevices.value) {
    charts.device = charts.device || echarts.init(deviceEl.value)
    charts.device.setOption(pieOption(devices.value), true)
  }
  if (refererEl.value && hasReferers.value) {
    charts.referer = charts.referer || echarts.init(refererEl.value)
    charts.referer.setOption(barOption(refererRows(), 5, refererTooltip), true)
  }
  if (regionEl.value && hasRegions.value) {
    charts.region = charts.region || echarts.init(regionEl.value)
    charts.region.setOption(barOption(regions.value, 1), true)
  }
}

function onResize() {
  Object.values(charts).forEach((c) => c?.resize())
}

onMounted(() => {
  loadData()
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  Object.values(charts).forEach((c) => c?.dispose())
  charts = {}
})
</script>

<style scoped>
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}

.section-head-left {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.section-head-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.section-head h2 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
}

.section-tip,
.panel-tip {
  font-size: 12px;
  color: var(--color-text-muted);
}

.stat-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 12px;
}

.stat-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  padding: 14px 16px;
}

.stat-card__label {
  font-size: 12px;
  color: var(--color-text-muted);
}

.stat-card__value {
  margin-top: 6px;
  font-size: 26px;
  font-weight: 600;
  line-height: 1.2;
}

.stat-card__sub {
  margin-top: 4px;
  font-size: 12px;
  color: var(--color-text-muted);
}

.chart-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  margin-top: 12px;
}

.panel {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  padding: 14px 16px 8px;
}

.panel-head {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 6px;
  flex-wrap: wrap;
}

.panel-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text);
}

.chart {
  height: 240px;
}

.chart--tall {
  height: 280px;
}

.chart-empty {
  padding: 70px 0;
}

@media (max-width: 1100px) {
  .stat-row {
    grid-template-columns: repeat(2, 1fr);
  }

  .chart-grid {
    grid-template-columns: 1fr;
  }
}
</style>
