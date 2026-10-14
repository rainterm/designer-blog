import { defineStore } from 'pinia'
import { DEFAULT_LAYOUT_MODE, LAYOUT_OPTIONS } from '@/constants/layout'
import {
  DEFAULT_PRIMARY,
  applyThemeCssVars,
  normalizePrimaryColor
} from '@/theme'

const STORAGE_KEY = 'designer-admin-app'
const LAYOUT_VALUES = new Set(LAYOUT_OPTIONS.map((o) => o.value))

function loadPersisted() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return {}
    const data = JSON.parse(raw)
    return data && typeof data === 'object' ? data : {}
  } catch {
    return {}
  }
}

function normalizeLayoutMode(mode) {
  return LAYOUT_VALUES.has(mode) ? mode : DEFAULT_LAYOUT_MODE
}

function savePersisted(state) {
  localStorage.setItem(
    STORAGE_KEY,
    JSON.stringify({
      collapsed: !!state.collapsed,
      layoutMode: normalizeLayoutMode(state.layoutMode),
      primaryColor: normalizePrimaryColor(state.primaryColor),
      visitedViews: state.visitedViews || [],
      expandedKeys: state.expandedKeys || []
    })
  )
}

/**
 * 应用状态：侧边栏折叠、菜单展开、多页签（刷新后保持）
 */
export const useAppStore = defineStore('app', {
  state: () => {
    const saved = loadPersisted()
    return {
      collapsed: !!saved.collapsed,
      /** 页面布局模式 */
      layoutMode: normalizeLayoutMode(saved.layoutMode),
      /** 主题主色 */
      primaryColor: normalizePrimaryColor(saved.primaryColor),
      /** 偏好设置面板是否打开 */
      preferenceVisible: false,
      /** 侧栏已展开的目录 key */
      expandedKeys: Array.isArray(saved.expandedKeys) ? saved.expandedKeys : [],
      /** 已打开的页签 */
      visitedViews: Array.isArray(saved.visitedViews) ? saved.visitedViews : []
    }
  },

  actions: {
    persist() {
      savePersisted(this.$state)
    },

    toggleCollapsed() {
      this.collapsed = !this.collapsed
      this.persist()
    },

    setLayoutMode(mode) {
      const next = normalizeLayoutMode(mode)
      if (next === this.layoutMode) return
      this.layoutMode = next
      this.persist()
    },

    setPrimaryColor(color) {
      const next = normalizePrimaryColor(color)
      if (next === this.primaryColor) return
      this.primaryColor = next
      applyThemeCssVars(next)
      this.persist()
    },

    openPreference() {
      this.preferenceVisible = true
    },

    closePreference() {
      this.preferenceVisible = false
    },

    /** 重置布局 + 主题偏好 */
    resetPreference() {
      this.layoutMode = DEFAULT_LAYOUT_MODE
      this.primaryColor = DEFAULT_PRIMARY
      applyThemeCssVars(DEFAULT_PRIMARY)
      this.persist()
    },

    /** @deprecated 使用 resetPreference */
    resetLayoutPreference() {
      this.resetPreference()
    },

    setExpandedKeys(keys) {
      const next = Array.isArray(keys) ? keys : []
      // 避免无意义写入触发多余序列化
      if (
        next.length === this.expandedKeys.length &&
        next.every((k, i) => k === this.expandedKeys[i])
      ) {
        return
      }
      this.expandedKeys = next
      this.persist()
    },

    addVisitedView(route) {
      if (!route.name || route.meta?.hidden || route.name === 'Login') return
      if (this.visitedViews.some((v) => v.path === route.path)) return
      this.visitedViews.push({
        name: route.name,
        path: route.path,
        title: route.meta?.title || route.name,
        icon: route.meta?.icon || '',
        affix: !!route.meta?.affix
      })
      this.persist()
    },

    /** 根据路由表初始化固定页签（meta.affix === true） */
    initAffixViews(routes) {
      for (const r of routes || []) {
        if (!r.meta?.affix || !r.name) continue
        this.addVisitedView({
          name: r.name,
          path: r.path,
          meta: r.meta
        })
      }
    },

    removeVisitedView(path) {
      const target = this.visitedViews.find((v) => v.path === path)
      if (target?.affix) return
      this.visitedViews = this.visitedViews.filter((v) => v.path !== path)
      this.persist()
    },

    removeOtherViews(path) {
      this.visitedViews = this.visitedViews.filter((v) => v.affix || v.path === path)
      this.persist()
    },

    clearViews() {
      this.visitedViews = this.visitedViews.filter((v) => v.affix)
      this.persist()
    },

    /** 登出时清理页签与菜单展开（折叠偏好保留） */
    resetLayout() {
      this.visitedViews = []
      this.expandedKeys = []
      this.persist()
    }
  }
})
