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
          <router-view />
        </div>
      </n-dialog-provider>
    </n-message-provider>
  </n-config-provider>
</template>

<script setup>
import { zhCN, dateZhCN } from 'naive-ui'
import { themeOverrides } from '@/config/theme'
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
