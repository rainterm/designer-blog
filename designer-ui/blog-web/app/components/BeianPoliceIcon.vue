<template>
  <!--
    公安网安备案的官方图标（备案平台「我的网站 → 详情」下载的「备案编号图标」）。
    官方示例代码给的是 width="20"，但那是给正文页脚用的尺度；
    本站页脚字号只有 12px，20px 的图标会压过文字一头，故按字号收到 14px。
    图标原图是 62×67 的非正方形，用 object-fit: contain 保证不变形。
  -->
  <img
    v-if="!failed"
    ref="imgRef"
    class="beian-police"
    :src="iconSrc"
    alt="公安备案"
    width="14"
    height="14"
    @error="failed = true"
  />
</template>

<script setup>
import { onMounted, ref } from 'vue'

/**
 * 必须用绑定的 `:src`，不能写死 `src="/beian-police.png"`：
 * Vite 会把模板里静态的绝对路径当工程内资源去解析，解析不到就改写成
 * `/_nuxt/@fs/beian-police.png`，public 下真的放了图也照样 404。
 * 绑定到变量后 Vue 只负责写属性，路径原样交给浏览器。
 */
const iconSrc = '/beian-police.png'

/**
 * 图标文件缺失时不留下一个裂图：加载失败就把自己摘掉，页脚退化为「只有备案号文字」。
 * 只靠 @error 不够 —— 图写在 SSR 的 HTML 里，404 可能发生在客户端水合之前，事件收不到。
 * 用 decode() 兜底：它不管加载何时结束，最终都会以 reject 告诉我们是坏图。
 */
const failed = ref(false)
const imgRef = ref(null)

onMounted(() => {
  const img = imgRef.value
  if (!img) return

  const check = () => {
    if (img.complete && img.naturalWidth === 0) {
      failed.value = true
    }
  }

  check()
  if (typeof img.decode === 'function') {
    img.decode().then(check, () => {
      failed.value = true
    })
  } else {
    img.addEventListener('load', check)
    img.addEventListener('error', check)
  }
})
</script>

<style scoped>
.beian-police {
  /* 与 .footer-police 的 gap 配套：图标边长跟 12px 正文字号对齐，略高于正文但不超过行高 */
  width: 14px;
  height: 14px;
  flex-shrink: 0;
  /* 原图 62×67 略高于宽，contain 等比缩放，不会压扁 */
  object-fit: contain;
}
</style>
