<template>
  <PostList :category-slug="slug" />
</template>

<script setup>
import PostList from '@/components/PostList.vue'

const route = useRoute()
const slug = computed(() => String(route.params.slug || ''))
</script>
