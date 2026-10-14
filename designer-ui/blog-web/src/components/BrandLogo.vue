<template>
  <span class="brand-logo" :style="rootStyle">
    <img v-if="src" class="brand-logo__media" :src="src" :alt="alt" />
    <svg
      v-else
      class="brand-logo__media"
      viewBox="0 0 64 64"
      fill="none"
      role="img"
      :aria-label="alt"
    >
      <rect class="brand-logo-bg" width="64" height="64" />
      <path
        fill="#ffffff"
        fill-rule="evenodd"
        d="M20 14h14c11.5 0 20 8 20 18s-8.5 18-20 18H20V14zm9 9v18h5c6.2 0 11-4.7 11-9s-4.8-9-11-9H29z"
      />
    </svg>
  </span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  /** 站点 Logo 图片地址；为空则渲染内置 SVG */
  src: { type: String, default: '' },
  /** 圆角（px），0 为直角 */
  radius: { type: Number, default: 6 },
  alt: { type: String, default: 'logo' }
})

/**
 * 圆角由外层容器裁剪（overflow: hidden + border-radius），
 * 这样上传图片与内置 SVG 的圆角表现完全一致。
 */
const rootStyle = computed(() => ({ borderRadius: `${Math.max(0, props.radius)}px` }))
</script>

<style scoped>
.brand-logo {
  display: block;
  flex-shrink: 0;
  overflow: hidden;
  /* 尺寸由使用方控制（如 .brand__logo 的 width/height） */
}

.brand-logo__media {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.brand-logo-bg {
  fill: var(--color-primary);
}
</style>
