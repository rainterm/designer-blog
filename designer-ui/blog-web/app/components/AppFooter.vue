<template>
  <footer v-if="hasContent" class="app-footer">
    <div class="page-container footer-inner">
      <p v-if="site.copyright" class="footer-line">{{ site.copyright }}</p>
      <p v-if="site.icp" class="footer-line">
        <a
          v-if="site.icpUrl"
          class="footer-link"
          :href="site.icpUrl"
          target="_blank"
          rel="noopener noreferrer"
        >
          {{ site.icp }}
        </a>
        <span v-else>{{ site.icp }}</span>
      </p>
    </div>
  </footer>
</template>

<script setup>
import { useSiteSetting } from '@/composables/useSiteSetting'

const site = useSiteSetting()

/**
 * 版权 / 备案号都为空时不渲染页脚：否则会留下一条空的分隔线与空白高度，
 * 视觉上像是「版权被吃掉了」。只有后台真的配了内容才出现。
 */
const hasContent = computed(() => Boolean(site.value.copyright || site.value.icp))
</script>

<style scoped>
/* 高度与顶栏一致，首尾对称 */
.app-footer {
  min-height: var(--layout-nav-height);
  display: flex;
  align-items: center;
  justify-content: center;
  border-top: 1px solid var(--color-border);
  margin-top: 16px;
  padding: 12px 0;
  color: var(--color-text-muted);
  font-size: 12px;
  text-align: center;
}

.footer-inner {
  display: flex;
  flex-direction: column;
  gap: 2px;
  align-items: center;
}

.footer-line {
  line-height: 1.6;
}

.footer-link {
  transition: color 0.15s;
}

.footer-link:hover {
  color: var(--color-primary);
}
</style>
