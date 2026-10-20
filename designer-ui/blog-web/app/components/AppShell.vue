<template>
  <n-config-provider
    class="app-shell"
    :locale="zhCN"
    :date-locale="dateZhCN"
    :theme="isDark ? darkTheme : null"
    :theme-overrides="themeOverrides"
  >
    <n-message-provider>
      <n-dialog-provider>
        <div class="app-view" :style="themeCssVars">
          <slot />
        </div>
      </n-dialog-provider>
    </n-message-provider>
  </n-config-provider>
</template>

<script setup>
import { darkTheme, zhCN, dateZhCN } from 'naive-ui'
import {
  colors,
  darkColors,
  themeOverrides as lightThemeOverrides,
  darkThemeOverrides,
  normalizePrimaryColor,
  buildThemeOverrides,
  themeColorVars
} from '@/config/theme'
import { useColorMode } from '@/composables/useColorMode'
import { useSiteSetting } from '@/composables/useSiteSetting'

/**
 * 应用外壳：主题 + 全局消息/对话框上下文。
 *
 * <p>抽成组件是因为 `error.vue` 与 `app.vue` 在 Nuxt 里是**互斥**的：
 * nuxt-root 用 `v-if="abortRender" / v-else-if="error"` 二选一渲染
 * ErrorComponent 或 AppComponent（见 nuxt/dist/app/components/nuxt-root.vue）。
 * 也就是说出错时 app.vue 整体不参与渲染，provider 会一并消失 ——
 * 错误页里的 n-button 就会因为找不到上下文而报错。两边共用同一个外壳即可。</p>
 */
const site = useSiteSetting()
const { isDark } = useColorMode()

/**
 * 生效的站点主题色（后台「站点设置 → 站点主题色」下发）。
 *
 * <p>为空 / 非法 / 等于内置默认时都返回空串 —— 等于默认也刻意不覆盖：
 * {@link buildColors} 的派生公式与 tokens.css 里手绘的色板并非逐位相同
 * （如 primaryPressed / primaryLight），覆盖会让默认观感发生肉眼可见的变化。
 * 只有站长确实选了别的颜色才接管。</p>
 */
const customColor = computed(() => {
  const raw = site.value.themeColor
  if (!raw) return ''
  const color = normalizePrimaryColor(raw)
  return color === colors.primary ? '' : color
})

const mode = computed(() => (isDark.value ? 'dark' : 'light'))

const themeOverrides = computed(() => {
  if (customColor.value) return buildThemeOverrides(customColor.value, mode.value)
  return isDark.value ? darkThemeOverrides : lightThemeOverrides
})

/**
 * 主题色 CSS 变量写到内容容器上而不是 `:root`：
 * 内联样式随 SSR 首屏 HTML 一起下发（爬虫与访客看到同一份，无水合差异），
 * 又因为是「更近的祖先」，天然胜过 tokens.css 里 `:root` 的定义，不必纠结样式顺序。
 *
 * <p>夜间模式下这里只覆盖品牌色；body / surface / text 等仍由
 * `:root[data-theme='dark']` 提供，否则内联变量会把整套深色底色盖掉。</p>
 */
const themeCssVars = computed(() =>
  customColor.value ? themeColorVars(customColor.value, mode.value) : null
)

/**
 * 移动端地址栏 / 状态栏的底色。
 * 交给 unhead 渲染，SSR 就带着正确的值（按 Cookie 判断），不会先亮后暗。
 */
useHead({
  meta: computed(() => [
    {
      name: 'theme-color',
      content: isDark.value ? darkColors.body : colors.body,
      key: 'theme-color'
    }
  ])
})
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
