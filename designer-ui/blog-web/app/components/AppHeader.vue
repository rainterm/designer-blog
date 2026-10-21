<template>
  <header class="app-header">
    <div class="app-header__inner page-container">
      <NuxtLink to="/" class="brand">
        <BrandLogo
          class="brand__logo"
          :src="site.logoUrl"
          :radius="site.logoRadius"
          :alt="site.siteName"
        />
        <span class="brand__name">{{ site.siteName }}</span>
        <span v-if="site.siteSubtitle" class="brand__subtitle">{{ site.siteSubtitle }}</span>
      </NuxtLink>

      <div class="app-header__actions">
        <nav v-if="links.length" class="social" aria-label="个人链接">
          <a
            v-for="l in links"
            :key="l.id"
            class="social__item"
            :href="l.url"
            target="_blank"
            rel="noopener noreferrer"
            :title="l.name"
            :aria-label="l.name"
          >
            <LinkIcon :name="l.icon" />
          </a>
        </nav>

        <form v-if="showSearch" class="search" @submit.prevent="emitSearch">
          <input
            v-model="keyword"
            class="search__input"
            type="search"
            placeholder="搜索文章"
            enterkeyhint="search"
            autocomplete="off"
            @keyup.enter="emitSearch"
          />
          <button
            v-if="keyword"
            type="button"
            class="search__clear"
            aria-label="清除"
            @click="clearSearch"
          >
            ×
          </button>
          <!-- 提交按钮：移动端没有键盘「搜索」键的直观提示，点这里即可触发 -->
          <button type="submit" class="search__submit" aria-label="搜索" title="搜索">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <circle cx="11" cy="11" r="7" fill="none" stroke="currentColor" stroke-width="2" />
              <path d="M20 20l-3.5-3.5" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
            </svg>
          </button>
        </form>

        <!--
          主题选择：跟随系统 / 日间 / 夜间。

          三态而不是两态，是因为「跟随系统」本身就是一个要能被选中的状态 ——
          两态开关只能表达「我选了日间」，用户点过一次之后就再也没法回到
          「系统变我也变」。这也是 GitHub / Tailwind 文档等站点的通行做法。

          按钮图标显示的是**当前实际生效**的模式（system 且系统为夜间时显示太阳），
          菜单里的对勾显示的才是**用户选的偏好**，两者刻意分开。
        -->
        <n-dropdown
          trigger="click"
          placement="bottom-end"
          :show-arrow="false"
          :options="themeOptions"
          @select="setPreference"
        >
          <button
            type="button"
            class="theme-toggle"
            :aria-label="themeLabel"
            :title="themeLabel"
          >
            <svg v-if="isDark" class="theme-toggle__icon" viewBox="0 0 24 24" aria-hidden="true">
              <circle cx="12" cy="12" r="4" fill="none" stroke="currentColor" stroke-width="2" />
              <path
                d="M12 2v2m0 16v2M4.9 4.9l1.4 1.4m11.4 11.4 1.4 1.4M2 12h2m16 0h2M4.9 19.1l1.4-1.4m11.4-11.4 1.4-1.4"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
              />
            </svg>
            <svg v-else class="theme-toggle__icon" viewBox="0 0 24 24" aria-hidden="true">
              <path
                d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linejoin="round"
              />
            </svg>
          </button>
        </n-dropdown>
      </div>
    </div>
  </header>
</template>

<script setup>
import { h } from 'vue'
import BrandLogo from '@/components/BrandLogo.vue'
import LinkIcon from '@/components/LinkIcon.vue'
import { useBlogLinks, useSiteSetting } from '@/composables/useSiteSetting'
import { useColorMode } from '@/composables/useColorMode'

const route = useRoute()
const router = useRouter()
const site = useSiteSetting()
const { preference, isDark, setPreference } = useColorMode()

/** 三个选项；对勾标在**当前偏好**上，而不是标在当前外观上 */
const THEME_OPTIONS = [
  { key: 'system', label: '跟随系统' },
  { key: 'light', label: '日间' },
  { key: 'dark', label: '夜间' }
]

const themeOptions = computed(() =>
  THEME_OPTIONS.map((item) => ({
    key: item.key,
    label: item.label,
    icon: () =>
      h(
        'span',
        { class: 'theme-menu__check' },
        preference.value === item.key ? '✓' : ''
      )
  }))
)

/** 悬停提示同时说清「当前生效」与「当前偏好」，system 下两者可能不同 */
const themeLabel = computed(() => {
  const current = preference.value === 'system' ? '跟随系统' : isDark.value ? '夜间' : '日间'
  return `主题：${current}`
})
// 外链由 app/plugins/blog-config.server.js 在服务端拉好，随 payload 一起下发，
// 这里只管读 —— 不再需要 onMounted 里的 load()
const links = useBlogLinks()
const keyword = ref(typeof route.query.q === 'string' ? route.query.q : '')

// 文章详情页没有搜索框（在正文里搜索无意义），其余列表页都有
const showSearch = computed(() => !route.path.startsWith('/post/'))

watch(
  () => route.query.q,
  (q) => {
    keyword.value = typeof q === 'string' ? q : ''
  }
)

function emitSearch() {
  const q = keyword.value.trim()
  const query = { ...route.query }
  if (q) query.q = q
  else delete query.q
  // 换关键词要回到第一页，否则会停留在上一次搜索的页码上
  delete query.page
  router.push({ path: route.path, query })
}

function clearSearch() {
  keyword.value = ''
  emitSearch()
}
</script>

<style scoped>
.app-header {
  position: sticky;
  top: 0;
  z-index: 50;
  height: var(--layout-nav-height);
  background: var(--color-header-bg);
  backdrop-filter: saturate(180%) blur(12px);
  -webkit-backdrop-filter: saturate(180%) blur(12px);
  border-bottom: 1px solid var(--color-header-border);
  display: flex;
  align-items: center;
  transition: background-color 0.2s ease, border-color 0.2s ease;
}

.app-header__inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  height: 100%;
  /* 窄屏下允许子项按剩余宽度收缩，避免主题按钮被裁切 */
  min-width: 0;
}

.app-header__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  min-width: 0;
  /* 占满品牌右侧剩余空间，搜索框才能按可用宽度收缩 */
  flex: 1 1 auto;
}

.social {
  display: flex;
  align-items: center;
  gap: 2px;
  flex-shrink: 0;
}

.social__item {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: var(--radius-full);
  color: var(--color-text-secondary);
  transition: color 0.15s, background 0.15s;
}

.social__item:hover {
  color: var(--color-primary);
  background: var(--color-primary-soft);
}

/* 夜间模式开关：与左侧社交图标同尺寸，但永远是单个按钮 */
.theme-toggle {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  padding: 0;
  border: none;
  border-radius: var(--radius-full);
  background: transparent;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: color 0.15s, background 0.15s;
}

.theme-toggle:hover {
  color: var(--color-primary);
  background: var(--color-primary-soft);
}

.theme-toggle__icon {
  width: 18px;
  height: 18px;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 1;
  min-width: 0;
  color: var(--color-text);
  line-height: 1;
}

.brand__logo {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
}

.brand__name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.brand__subtitle {
  font-size: 12px;
  font-weight: 400;
  color: var(--color-text-muted);
  padding-left: 10px;
  margin-left: 2px;
  border-left: 1px solid var(--color-border);
  line-height: 1.2;
}

.search {
  display: flex;
  align-items: center;
  gap: 6px;
  /* 用 flex 吃剩余宽度，而不是写死 vw：否则窄屏上社交图标一多就把主题按钮挤出视口 */
  flex: 1 1 auto;
  min-width: 0;
  width: auto;
  max-width: 260px;
  /* 34px + 顶栏 42px：上下各留 4px，输入框才不会顶到分隔线 */
  height: 34px;
  padding: 0 5px 0 14px;
  border-radius: var(--radius);
  /* 用「比画布亮/暗一档」的填充色而不是写死 body 色：
     亮色下与 body 相同（视觉不变），深色下提亮到 7% 白，顶栏里才看得出是个输入框 */
  background: var(--color-search-bg);
  border: 1px solid transparent;
  transition: background 0.15s, border-color 0.15s, box-shadow 0.15s;
}

.search:focus-within {
  background: var(--color-surface);
  border-color: color-mix(in srgb, var(--color-primary) 35%, transparent);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--color-primary) 12%, transparent);
}

.search__submit {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  padding: 0;
  border: none;
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--color-text-muted);
  cursor: pointer;
  transition: color 0.15s, background 0.15s;
}

.search__submit svg {
  width: 16px;
  height: 16px;
}

.search__submit:hover,
.search:focus-within .search__submit {
  color: var(--color-primary);
}

.search__submit:hover {
  background: color-mix(in srgb, var(--color-primary) 10%, transparent);
}

.search__input {
  flex: 1;
  min-width: 0;
  border: none;
  outline: none;
  background: transparent;
  font-family: inherit;
  font-size: 13px;
  color: var(--color-text);
  line-height: 1;
}

.search__input::placeholder {
  color: var(--color-text-muted);
}

.search__input::-webkit-search-cancel-button {
  display: none;
}

.search__clear {
  flex-shrink: 0;
  width: 20px;
  height: 20px;
  border: none;
  border-radius: 50%;
  background: var(--color-border);
  color: var(--color-text-secondary);
  font-size: 14px;
  line-height: 1;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0;
}

.search__clear:hover {
  background: var(--color-border-strong);
  color: var(--color-text);
}

/* 窄屏：收紧间距，搜索让位给主题按钮，避免右侧被裁切 */
@media (max-width: 600px) {
  .app-header__inner {
    gap: 8px;
  }

  .app-header__actions {
    gap: 2px;
    /* 优先保住右侧操作区，站点名过长时由 brand 侧省略 */
    flex: 1 1 0;
  }

  .brand {
    flex: 0 1 auto;
    max-width: 46%;
  }

  .social__item {
    width: 30px;
    height: 30px;
  }

  .theme-toggle {
    width: 30px;
    height: 30px;
  }

  /* 窄屏隐藏副标题，优先保证站点名与搜索框 */
  .brand__subtitle {
    display: none;
  }

  .search {
    flex: 1 1 0;
    max-width: 150px;
    padding-left: 10px;
  }

  /* 触屏上把点击区域放大到 ~30px，避免图标太小点不中 */
  .search__submit {
    width: 30px;
    height: 30px;
  }
}
</style>

<style>
/*
 * 主题菜单的对勾。
 *
 * <p>菜单由 naive-ui 传送到 body 下，拿不到 scoped 的作用域属性，只能用全局样式。
 * 类名带 `theme-menu` 前缀，避免与站点其他样式撞车。</p>
 *
 * <p>固定宽度 + 居中对齐：未选中的项也占住同样的位置，三个选项的文字才能对齐。</p>
 */
.theme-menu__check {
  display: inline-block;
  width: 1em;
  text-align: center;
  color: var(--color-primary);
  font-weight: 600;
}
</style>
