<template>
  <n-drawer
    :show="appStore.preferenceVisible"
    :width="420"
    placement="right"
    :trap-focus="false"
    @update:show="onVisible"
  >
    <n-drawer-content :native-scrollbar="false" closable class="pref-drawer">
      <template #header>
        <div class="pref-header">
          <div class="pref-header__text">
            <div class="pref-header__title">偏好设置</div>
            <div class="pref-header__sub">自定义偏好设置 & 实时预览</div>
          </div>
          <div class="pref-header__actions">
            <n-tooltip :show-arrow="false">
              <template #trigger>
                <n-button quaternary circle @click="resetPreference">
                  <template #icon>
                    <n-icon :component="RefreshOutline" :size="18" />
                  </template>
                </n-button>
              </template>
              重置偏好
            </n-tooltip>
          </div>
        </div>
      </template>

      <div class="pref-tabs">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          type="button"
          class="pref-tab"
          :class="{ 'is-active': activeTab === tab.key }"
          @click="activeTab = tab.key"
        >
          {{ tab.label }}
        </button>
      </div>

      <div v-if="activeTab === 'appearance'" class="pref-section">
        <div class="pref-section__title">主题色</div>
        <div class="color-row">
          <button
            v-for="preset in PRIMARY_PRESETS"
            :key="preset.value"
            type="button"
            class="color-swatch"
            :class="{ 'is-active': isSameColor(appStore.primaryColor, preset.value) }"
            :style="{ background: preset.value }"
            :title="preset.label"
            @click="selectPrimary(preset.value)"
          >
            <n-icon
              v-if="isSameColor(appStore.primaryColor, preset.value)"
              class="color-swatch__check"
              :component="CheckmarkOutline"
              :size="16"
            />
          </button>
          <n-color-picker
            class="color-picker"
            :value="appStore.primaryColor"
            :modes="['hex']"
            :show-alpha="false"
            @update:value="selectPrimary"
          />
        </div>
        <div class="color-current">
          当前：{{ currentPresetLabel }}
          <span class="color-current__hex">{{ appStore.primaryColor }}</span>
        </div>
      </div>

      <div v-else-if="activeTab === 'layout'" class="pref-section">
        <div class="pref-section__title">布局</div>
        <div class="layout-grid">
          <button
            v-for="opt in LAYOUT_OPTIONS"
            :key="opt.value"
            type="button"
            class="layout-card"
            :class="{ 'is-active': appStore.layoutMode === opt.value }"
            @click="selectLayout(opt.value)"
          >
            <div class="layout-card__preview" :data-mode="opt.value">
              <span class="lp-sider" />
              <span class="lp-sider2" />
              <span class="lp-header" />
              <span class="lp-body">
                <i /><i /><i />
              </span>
            </div>
            <div class="layout-card__label">
              <span>{{ opt.label }}</span>
              <n-tooltip :show-arrow="false" placement="top">
                <template #trigger>
                  <n-icon class="layout-card__tip" :component="HelpCircleOutline" :size="14" />
                </template>
                {{ opt.tip }}
              </n-tooltip>
            </div>
          </button>
        </div>
      </div>
    </n-drawer-content>
  </n-drawer>
</template>

<script setup>
import { computed, ref } from 'vue'
import { CheckmarkOutline, HelpCircleOutline, RefreshOutline } from '@vicons/ionicons5'
import { useAppStore } from '@/store/app'
import { LAYOUT_OPTIONS } from '@/constants/layout'
import { PRIMARY_PRESETS, normalizePrimaryColor } from '@/theme'

const appStore = useAppStore()
const activeTab = ref('appearance')

const tabs = [
  { key: 'appearance', label: '外观' },
  { key: 'layout', label: '布局' }
]

const currentPresetLabel = computed(() => {
  const hit = PRIMARY_PRESETS.find((p) => isSameColor(appStore.primaryColor, p.value))
  return hit ? hit.label : '自定义'
})

function isSameColor(a, b) {
  return normalizePrimaryColor(a) === normalizePrimaryColor(b)
}

function onVisible(show) {
  if (show) appStore.openPreference()
  else appStore.closePreference()
}

function selectLayout(mode) {
  appStore.setLayoutMode(mode)
}

function selectPrimary(color) {
  if (!color) return
  appStore.setPrimaryColor(color)
}

function resetPreference() {
  appStore.resetPreference()
}
</script>

<style scoped>
.pref-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
  padding-right: 28px;
}

.pref-header__title {
  font-size: 18px;
  font-weight: 700;
  line-height: 1.3;
  color: var(--color-text);
}

.pref-header__sub {
  margin-top: 2px;
  font-size: 12px;
  color: var(--color-text-muted);
}

.pref-header__actions {
  display: flex;
  align-items: center;
  gap: 2px;
  flex-shrink: 0;
}

.pref-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 4px;
  margin-bottom: 20px;
  border-radius: var(--radius);
  background: color-mix(in srgb, var(--color-body) 85%, var(--color-border));
}

.pref-tab {
  flex: 1 1 auto;
  min-width: 56px;
  height: 32px;
  padding: 0 10px;
  border: none;
  border-radius: calc(var(--radius) - 1px);
  background: transparent;
  color: var(--color-text-secondary);
  font-size: 13px;
  cursor: pointer;
  transition: background-color 0.15s, color 0.15s, box-shadow 0.15s;
}

.pref-tab:hover {
  color: var(--color-text);
}

.pref-tab.is-active {
  background: var(--color-surface);
  color: var(--color-primary);
  font-weight: 600;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.06);
}

.pref-section__title {
  margin-bottom: 12px;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text);
}

.color-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.color-swatch {
  position: relative;
  width: 28px;
  height: 28px;
  padding: 0;
  border: 2px solid transparent;
  border-radius: 50%;
  cursor: pointer;
  box-shadow: inset 0 0 0 1px rgba(0, 0, 0, 0.08);
  transition: transform 0.15s, box-shadow 0.15s;
}

.color-swatch:hover {
  transform: scale(1.08);
}

.color-swatch.is-active {
  box-shadow:
    0 0 0 2px var(--color-surface),
    0 0 0 4px var(--color-primary);
}

.color-swatch__check {
  position: absolute;
  inset: 0;
  margin: auto;
  color: #fff;
  filter: drop-shadow(0 1px 1px rgba(0, 0, 0, 0.25));
}

.color-picker {
  width: 28px;
  height: 28px;
}

.color-picker :deep(.n-color-picker-trigger) {
  width: 28px;
  height: 28px;
  min-width: 28px;
  padding: 0;
  border-radius: 50%;
  overflow: hidden;
}

.color-picker :deep(.n-color-picker-trigger__fill) {
  border-radius: 50%;
  inset: 2px;
}

.color-current {
  margin-top: 12px;
  font-size: 12px;
  color: var(--color-text-secondary);
}

.color-current__hex {
  margin-left: 6px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  color: var(--color-text-muted);
}

.layout-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.layout-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 0;
  border: none;
  background: transparent;
  cursor: pointer;
  text-align: left;
}

.layout-card__preview {
  position: relative;
  height: 64px;
  border: 2px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-body);
  overflow: hidden;
  transition: border-color 0.15s, box-shadow 0.15s;
}

.layout-card:hover .layout-card__preview {
  border-color: color-mix(in srgb, var(--color-primary) 45%, var(--color-border));
}

.layout-card.is-active .layout-card__preview {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 1px color-mix(in srgb, var(--color-primary) 25%, transparent);
}

.layout-card__label {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  font-size: 12px;
  color: var(--color-text-secondary);
}

.layout-card.is-active .layout-card__label {
  color: var(--color-primary);
  font-weight: 600;
}

.layout-card__tip {
  color: var(--color-text-muted);
  opacity: 0.85;
}

/* —— 布局缩略图几何 —— */
.lp-sider,
.lp-sider2,
.lp-header,
.lp-body {
  position: absolute;
  display: block;
  border-radius: 2px;
}

.lp-sider {
  display: none;
  background: var(--color-primary);
}

.lp-sider2 {
  display: none;
  background: color-mix(in srgb, var(--color-primary) 35%, #dfe5e1);
}

.lp-header {
  display: none;
  background: var(--color-primary);
}

.lp-body {
  inset: 8px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 0;
}

.lp-body i {
  display: block;
  height: 8px;
  border-radius: 2px;
  background: #d7ddd9;
}

.lp-body i:nth-child(2) {
  width: 72%;
}

.lp-body i:nth-child(3) {
  width: 88%;
  flex: 1;
  min-height: 14px;
}

/* 垂直：左侧菜单 */
.layout-card__preview[data-mode='vertical'] .lp-sider {
  display: block;
  left: 6px;
  top: 6px;
  bottom: 6px;
  width: 16px;
}
.layout-card__preview[data-mode='vertical'] .lp-header {
  display: block;
  left: 26px;
  right: 6px;
  top: 6px;
  height: 10px;
  background: #cfd6d2;
}
.layout-card__preview[data-mode='vertical'] .lp-body {
  left: 26px;
  top: 20px;
  right: 6px;
  bottom: 6px;
}

/* 双列 */
.layout-card__preview[data-mode='dual-column'] .lp-sider {
  display: block;
  left: 5px;
  top: 6px;
  bottom: 6px;
  width: 10px;
}
.layout-card__preview[data-mode='dual-column'] .lp-sider2 {
  display: block;
  left: 17px;
  top: 6px;
  bottom: 6px;
  width: 12px;
}
.layout-card__preview[data-mode='dual-column'] .lp-header {
  display: block;
  left: 33px;
  right: 6px;
  top: 6px;
  height: 10px;
  background: #cfd6d2;
}
.layout-card__preview[data-mode='dual-column'] .lp-body {
  left: 33px;
  top: 20px;
  right: 6px;
  bottom: 6px;
}

/* 水平 */
.layout-card__preview[data-mode='horizontal'] .lp-header {
  display: block;
  left: 6px;
  right: 6px;
  top: 6px;
  height: 14px;
}
.layout-card__preview[data-mode='horizontal'] .lp-body {
  left: 6px;
  right: 6px;
  top: 24px;
  bottom: 6px;
}

/* 侧边导航（细图标栏） */
.layout-card__preview[data-mode='side-nav'] .lp-sider {
  display: block;
  left: 6px;
  top: 20px;
  bottom: 6px;
  width: 10px;
}
.layout-card__preview[data-mode='side-nav'] .lp-header {
  display: block;
  left: 6px;
  right: 6px;
  top: 6px;
  height: 10px;
}
.layout-card__preview[data-mode='side-nav'] .lp-body {
  left: 20px;
  top: 20px;
  right: 6px;
  bottom: 6px;
}

/* 混合垂直 */
.layout-card__preview[data-mode='mixed-vertical'] .lp-header {
  display: block;
  left: 6px;
  right: 6px;
  top: 6px;
  height: 10px;
}
.layout-card__preview[data-mode='mixed-vertical'] .lp-sider {
  display: block;
  left: 6px;
  top: 20px;
  bottom: 6px;
  width: 16px;
  background: #cfd6d2;
}
.layout-card__preview[data-mode='mixed-vertical'] .lp-body {
  left: 26px;
  top: 20px;
  right: 6px;
  bottom: 6px;
}

/* 混合双列 */
.layout-card__preview[data-mode='mixed-dual'] .lp-header {
  display: block;
  left: 6px;
  right: 6px;
  top: 6px;
  height: 10px;
}
.layout-card__preview[data-mode='mixed-dual'] .lp-sider {
  display: block;
  left: 6px;
  top: 20px;
  bottom: 6px;
  width: 10px;
}
.layout-card__preview[data-mode='mixed-dual'] .lp-sider2 {
  display: block;
  left: 18px;
  top: 20px;
  bottom: 6px;
  width: 12px;
}
.layout-card__preview[data-mode='mixed-dual'] .lp-body {
  left: 34px;
  top: 20px;
  right: 6px;
  bottom: 6px;
}

/* 内容全屏 */
.layout-card__preview[data-mode='content-full'] .lp-body {
  inset: 8px;
}
</style>
