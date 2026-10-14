<template>
  <div class="blog-layout">
    <div ref="scrollRef" class="blog-scroll">
      <AppHeader />
      <main class="blog-main">
        <div class="page-container">
          <router-view />
        </div>
      </main>
      <AppFooter />
    </div>
  </div>
</template>

<script setup>
import { provide, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import AppHeader from './AppHeader.vue'
import AppFooter from './AppFooter.vue'

const scrollRef = ref(null)
const route = useRoute()

provide('blogScrollEl', scrollRef)

watch(
  () => route.fullPath,
  () => {
    scrollRef.value?.scrollTo({ top: 0 })
  }
)
</script>

<style scoped>
.blog-layout {
  height: 100%;
  overflow: hidden;
}

.blog-scroll {
  height: 100%;
  /* 作为文章大纲（MdCatalog）的滚动容器，需要是定位元素才能算出正确的跳转偏移 */
  position: relative;
  overflow-x: hidden;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: thin;
  scrollbar-color: rgba(0, 0, 0, 0.22) transparent;
  /* 让 footer 在内容不足时也贴底 */
  display: flex;
  flex-direction: column;
}

.blog-scroll > :deep(.app-header),
.blog-scroll > :deep(.app-footer) {
  flex: none;
}

/* 滚动条：轨道透明、无左边线 */
.blog-scroll::-webkit-scrollbar {
  width: 10px;
}

.blog-scroll::-webkit-scrollbar-track {
  background: transparent;
  border: none;
  box-shadow: none;
}

.blog-scroll::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.22);
  border-radius: 999px;
  border: none;
  background-clip: padding-box;
}

.blog-scroll::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.35);
}

.blog-main {
  /* 撑满剩余空间，使页脚始终贴底 */
  flex: 1 0 auto;
  padding-top: 32px;
  padding-bottom: 48px;
}
</style>
