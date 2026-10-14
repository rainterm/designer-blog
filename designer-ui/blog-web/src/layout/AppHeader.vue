<template>
  <header class="app-header">
    <div class="app-header__inner page-container">
      <router-link to="/" class="brand">
        <BrandLogo
          class="brand__logo"
          :src="site.logoUrl"
          :radius="site.logoRadius"
          :alt="site.siteName"
        />
        <span class="brand__name">{{ site.siteName }}</span>
        <span v-if="site.siteSubtitle" class="brand__subtitle">{{ site.siteSubtitle }}</span>
      </router-link>

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
          <svg class="search__icon" viewBox="0 0 24 24" aria-hidden="true">
            <circle cx="11" cy="11" r="7" fill="none" stroke="currentColor" stroke-width="2" />
            <path d="M20 20l-3.5-3.5" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
          </svg>
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
        </form>
      </div>
    </div>
  </header>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BrandLogo from '@/components/BrandLogo.vue'
import LinkIcon from '@/components/LinkIcon.vue'
import { useBlogLinks } from '@/composables/useBlogLinks'
import { useSiteSetting } from '@/composables/useSiteSetting'

const route = useRoute()
const router = useRouter()
const { links, load: loadLinks } = useBlogLinks()
const site = useSiteSetting()
const keyword = ref(typeof route.query.q === 'string' ? route.query.q : '')

onMounted(loadLinks)

const showSearch = computed(() =>
  ['blog', 'category', 'tag'].includes(String(route.name))
)

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
  router.push({ name: route.name === 'blog' ? 'blog' : route.name, params: route.params, query })
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
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: saturate(180%) blur(12px);
  -webkit-backdrop-filter: saturate(180%) blur(12px);
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  display: flex;
  align-items: center;
}

.app-header__inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  height: 100%;
}

.app-header__actions {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
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
  width: 34px;
  height: 34px;
  border-radius: var(--radius-full);
  color: var(--color-text-secondary);
  transition: color 0.15s, background 0.15s;
}

.social__item:hover {
  color: var(--color-primary);
  background: var(--color-primary-soft);
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
  color: var(--color-text);
  line-height: 1;
}

.brand__logo {
  width: 28px;
  height: 28px;
}

.brand__name {
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
  gap: 8px;
  width: min(260px, 48vw);
  height: 36px;
  padding: 0 12px 0 14px;
  border-radius: var(--radius);
  background: var(--color-body);
  border: 1px solid transparent;
  transition: background 0.15s, border-color 0.15s, box-shadow 0.15s;
}

.search:focus-within {
  background: #fff;
  border-color: color-mix(in srgb, var(--color-primary) 35%, transparent);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--color-primary) 12%, transparent);
}

.search__icon {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
  color: var(--color-text-muted);
}

.search:focus-within .search__icon {
  color: var(--color-primary);
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

/* 窄屏：收紧搜索框与图标间距，避免顶栏溢出 */
@media (max-width: 600px) {
  .app-header__inner {
    gap: 10px;
  }

  .app-header__actions {
    gap: 4px;
  }

  .social__item {
    width: 30px;
    height: 30px;
  }

  /* 窄屏隐藏副标题，优先保证站点名与搜索框 */
  .brand__subtitle {
    display: none;
  }

  .search {
    width: min(150px, 38vw);
  }
}
</style>
