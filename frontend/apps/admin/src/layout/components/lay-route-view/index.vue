<script setup lang="ts">
import { computed, type Component } from "vue";
import type { RouteLocationNormalizedLoaded } from "vue-router";

const props = defineProps<{
  component: Component;
  route: RouteLocationNormalizedLoaded;
}>();

// 通过响应式 props 传递 RouterView 的当前组件，避免扁平化路由在切换时复用上一次的内容实例。
const currentComponent = computed(() => props.component);
</script>

<template>
  <slot :component="currentComponent" :full-path="route.fullPath" />
</template>
