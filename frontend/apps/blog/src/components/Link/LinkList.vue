<template>
  <div>
    <SubTitle
      title="settings.links"
      icon="friends"
      :count="links.length"
      :uppercase="false"
    />
    <ul class="grid grid-cols-2 md:grid-cols-4 xl:grid-cols-6 gap-8">
      <template v-for="[i, link] of links.entries()" :key="i">
        <LinkCard
          :nick="link.nick"
          :link="link.link"
          :avatar="link.avatar"
          :description="link.description"
          :type="localizeLink(link.label)"
          :vip="link.label === 'links-badge-vip'"
        />
      </template>
    </ul>
  </div>
</template>

<script setup lang="ts">
import { Link } from '@/models/Article.class'
import { PropType } from 'vue'
import { localizeLink } from '@/utils/localization'
import { SubTitle } from '../Title'

defineProps({
  links: {
    type: Array as PropType<Link[]>,
    default: () => [],
    required: true
  }
})
</script>
