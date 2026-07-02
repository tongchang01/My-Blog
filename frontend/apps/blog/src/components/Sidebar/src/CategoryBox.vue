<template>
  <div :class="sidebarBoxClasses">
    <SubTitle :title="'titles.category_list'" icon="category" />
    <ul class="flex justify-event flex-wrap gap-2 pt-2 cursor-pointer">
      <template v-if="categories.length > 0">
        <li
          class="flex flex-row items-center hover:opacity-50"
          v-for="category in categories"
          :key="category.slug"
          @click="navigateToCategory(category.slug)"
        >
          <span
            class="bg-ob-deep-900 text-center px-3 py-1 rounded-tl-md rounded-bl-md text-sm"
            :style="gradientBackground(category.slug === activeCategory)"
          >
            {{ category.name }}
          </span>
          <b
            class="bg-ob-deep-900 text-ob text-center px-2 py-1 rounded-tr-md rounded-br-md text-sm opacity-70"
          >
            {{ category.count }}
          </b>
        </li>
      </template>
      <template v-else>
        <ob-skeleton tag="li" :count="10" height="20px" width="3rem" />
      </template>
    </ul>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { SubTitle } from '@/components/Title'
import { useCategoryStore } from '@/stores/category'
import { useAppStore } from '@/stores/app'
import { useRouter } from 'vue-router'

const props = defineProps({
  sidebarBox: {
    type: Boolean,
    default: true
  },
  activeCategory: {
    type: String,
    default: ''
  }
})

const appStore = useAppStore()
const categoryStore = useCategoryStore()
const loading = ref(true)
const router = useRouter()

const fetchData = async () => {
  await categoryStore.fetchCategories(appStore.locale)
  loading.value = false
}

const navigateToCategory = (slug: string) => {
  router.push({
    name: 'category-articles',
    params: { lang: appStore.locale, slug }
  })
}

onMounted(fetchData)

const sidebarBoxClasses = computed(() => ({
  'sidebar-box': props.sidebarBox
}))
const categories = computed(() => categoryStore.categories)
const gradientBackground = (active: boolean) => {
  return active
    ? {
        background: appStore.themeConfig.theme.header_gradient_css,
        color: '#fff',
        opacity: 1
      }
    : {}
}
</script>
