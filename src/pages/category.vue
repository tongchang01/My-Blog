<template>
  <div class="flex flex-col mt-20">
    <div class="post-header">
      <Breadcrumbs :current="pageTitle" />
      <h1 class="post-title text-white uppercase">
        {{ pageTitle }}
      </h1>
    </div>
    <div class="main-grid">
      <div class="relative">
        <div
          class="post-html bg-ob-deep-800 px-14 py-16 rounded-2xl shadow-xl block min-h-screen"
        ></div>
      </div>
      <div class="col-span-1">
        <Sidebar />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Sidebar } from '@/components/Sidebar'
import Breadcrumbs from '@/components/Breadcrumbs.vue'
import usePageTitle from '@/hooks/usePageTitle'

const { pageTitle } = usePageTitle()
</script>

<style lang="scss" scoped></style>
