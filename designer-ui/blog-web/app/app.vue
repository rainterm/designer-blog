<template>
  <AppShell>
    <NuxtLayout>
      <NuxtPage />
    </NuxtLayout>
  </AppShell>
</template>

<script setup>
import { useFaviconHref } from '@/composables/useFavicon'

const faviconHref = useFaviconHref()

// 固定 key：客户端把图标换成圆角版本时是改 href，不会多插一个 link
useHead(() => ({ link: [{ rel: 'icon', href: faviconHref.value, key: 'favicon' }] }))
</script>
