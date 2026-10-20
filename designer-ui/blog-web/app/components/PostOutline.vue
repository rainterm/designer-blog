<template>
  <nav class="post-outline" aria-label="文章大纲">
    <ol class="post-outline__list">
      <li v-for="(item, index) in items" :key="`${item.id}-${index}`" class="post-outline__item">
        <a
          class="post-outline__link"
          :class="{ 'post-outline__link--active': index === activeIndex }"
          :style="{ '--depth': item.depth }"
          :href="`#${item.id}`"
          :title="item.text"
          @click.prevent="emit('select', item, index)"
        >
          {{ item.text }}
        </a>
      </li>
    </ol>
  </nav>
</template>

<script setup>
/**
 * 文章大纲的纯展示组件：标题列表、当前项都由外部算好。
 *
 * <p>不用 md-editor-v3 的 MdCatalog，是因为它的高亮判定（`标题位置 &lt; offset-top`）
 * 和它自己点击时的滚动落点（`scroll-element-offset-top`）用的是同一根线，
 * 且判定是严格小于 —— 点完标题恰好落在线上，高亮就永远停在上一个标题。
 * 它还会用 `mdHeadingId` 反查标题元素，默认取标题原文，英文标题（RBAC / SSO）
 * 的 slug id 对不上，点了直接不滚。高亮既然是本页的核心交互，就自己算：
 * 点击与滚动高亮共用同一个偏移量，不存在对不上的可能。</p>
 *
 * <p>组件只负责渲染与派发点击，不碰滚动与状态 —— 移动端（正文上方的折叠块）
 * 与桌面端（左侧一列）用的是同一份数据和高亮，表现才能一致。</p>
 */
defineProps({
  /** `{ id, text, depth }`：depth 是相对最小标题层级的缩进层级 */
  items: {
    type: Array,
    default: () => []
  },
  /** 当前高亮项在 items 里的下标 */
  activeIndex: {
    type: Number,
    default: 0
  }
})

const emit = defineEmits(['select'])
</script>

<style scoped>
.post-outline__list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.post-outline__link {
  position: relative;
  display: block;
  /* 左侧留出 10px：2px 竖线 + 8px 与文字的间距；缩进按层级叠加 */
  padding: 5px 8px 5px calc(10px + var(--depth, 0) * 14px);
  font-size: 13px;
  line-height: 1.5;
  color: var(--color-text-secondary);
  text-decoration: none;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  transition: color 0.2s;
}

/**
 * 高亮竖线。
 *
 * <p>不能用 `border-left`：边框的高度跟着**盒子**走，含上下 5px padding，
 * 会比文字高出一截，看着像"多出来一段"。这里用伪元素画线，
 * 高度取 `1em`（等于字号），再垂直居中 —— 竖线与文字齐高。</p>
 */
.post-outline__link::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 0;
  width: 2px;
  height: 1em;
  border-radius: 1px;
  background-color: transparent;
  transform: translateY(-50%);
  transition: background-color 0.2s;
}

.post-outline__link:hover {
  color: var(--color-primary);
}

.post-outline__link--active {
  color: var(--color-primary);
  font-weight: 600;
}

.post-outline__link--active::before {
  background-color: var(--color-primary);
}
</style>
