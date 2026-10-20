<template>
  <footer v-if="hasContent" class="app-footer">
    <div class="page-container footer-inner">
      <!--
        版权 + ICP + 公安备案同排一行，多一项就多一个竖线分隔；
        顺序即阅读顺序，缺项时自动收拢（竖线挂在「后面那项」上，所以不会出现连续两道或行首竖线）。
      -->
      <p class="footer-line footer-meta">
        <span v-if="site.copyright" class="footer-meta__item">{{ site.copyright }}</span>

        <span v-if="site.icp" class="footer-meta__item">
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
        </span>

        <span v-if="site.policeIcp" class="footer-meta__item">
          <a
            v-if="site.policeIcpUrl"
            class="footer-link footer-police__link"
            :href="site.policeIcpUrl"
            target="_blank"
            rel="noopener noreferrer"
          >
            <BeianPoliceIcon />
            {{ site.policeIcp }}
          </a>
          <span v-else class="footer-police__link">
            <BeianPoliceIcon />
            {{ site.policeIcp }}
          </span>
        </span>
      </p>
    </div>
  </footer>
</template>

<script setup>
import { useSiteSetting } from '@/composables/useSiteSetting'

const site = useSiteSetting()

/**
 * 版权 / ICP 备案号 / 公安备案号都为空时不渲染页脚：否则会留下一条空的分隔线与空白高度，
 * 视觉上像是「版权被吃掉了」。只有后台真的配了内容才出现。
 */
const hasContent = computed(
  () => Boolean(site.value.copyright || site.value.icp || site.value.policeIcp)
)
</script>

<style scoped>
/*
 * 页脚固定 50px，不跟随顶栏高度。
 *
 * 原先写的是 calc(var(--layout-nav-height) / 2)，顶栏一改就会连带改变页脚高度 ——
 * 两者本来是独立的视觉量（页脚是收尾信息，与导航区承担的任务不同），
 * 解耦后调哪个都不会误伤另一个。
 */
.app-footer {
  min-height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-top: 1px solid var(--color-border);
  margin-top: 16px;
  padding: 6px 0;
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

/*
 * 版权与两类备案同排一行；放不下时整项换行（flex-wrap），
 * 三项长短不一，硬挤会把「粤公网安备 …」拆到两行更难读。
 */
.footer-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 4px 10px;
}

/*
 * 每个条目自己也是 flex：内部「竖线 + 文字」按中心对齐。
 * 若留作行内块，inline-flex 的链接会按基线对齐，多出一截下沉空间把行高从 19.2 撑到 21.8，
 * 页脚也就跟着高出 2px，凑不出干净的 32px。
 */
.footer-meta__item {
  display: inline-flex;
  align-items: center;
}

/* 竖线挂在「后一项」上：缺项时自然消失，不会留下连续两道 */
.footer-meta__item + .footer-meta__item::before {
  content: '';
  flex-shrink: 0;
  width: 1px;
  height: 10px;
  margin-right: 10px;
  background: var(--color-border);
}

/*
 * 换行后竖线会落在行首，像是多出来的一个符号。
 * 这个宽度以下三项必然排不进一行（实测约 470px 内容 + 页面左右留白），故整体隐藏竖线，只靠间距区分。
 */
@media (max-width: 560px) {
  .footer-meta__item + .footer-meta__item::before {
    display: none;
  }
}

.footer-link {
  transition: color 0.15s;
}

.footer-link:hover {
  color: var(--color-primary);
}

/* 公安备案：警徽 + 文案，同一行垂直居中 */
.footer-police__link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
</style>
