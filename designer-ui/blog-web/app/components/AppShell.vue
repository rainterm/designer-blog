<template>
  <n-config-provider
    class="app-shell"
    :locale="zhCN"
    :date-locale="dateZhCN"
    :theme-overrides="themeOverrides"
  >
    <n-message-provider>
      <n-dialog-provider>
        <div class="app-view">
          <slot />
        </div>
      </n-dialog-provider>
    </n-message-provider>
  </n-config-provider>
</template>

<script setup>
import { zhCN, dateZhCN } from 'naive-ui'
import { themeOverrides } from '@/config/theme'

/**
 * 应用外壳：主题 + 全局消息/对话框上下文。
 *
 * <p>抽成组件是因为 `error.vue` 与 `app.vue` 在 Nuxt 里是**互斥**的：
 * nuxt-root 用 `v-if="abortRender" / v-else-if="error"` 二选一渲染
 * ErrorComponent 或 AppComponent（见 nuxt/dist/app/components/nuxt-root.vue）。
 * 也就是说出错时 app.vue 整体不参与渲染，provider 会一并消失 ——
 * 错误页里的 n-button 就会因为找不到上下文而报错。两边共用同一个外壳即可。</p>
 */
</script>

<style>
.app-shell {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

/* Message/Dialog Provider 是 Fragment，实际子节点就是 .app-view */
.app-view {
  flex: 1;
  min-height: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.app-view > * {
  flex: 1;
  min-height: 0;
  height: 100%;
}
</style>
