<template>
  <nav class="items-center flex-1 hidden lg:flex">
    <ul class="flex flex-row items-center list-none px-6 text-white">
      <li
        class="not-italic font-medium text-xs h-full relative flex flex-col items-center justify-center cursor-pointer text-center py-2 px-2"
        v-for="route in routes"
        :key="route.path"
      >
        <a
          v-if="
            route.children &&
            route.children.length === 0 &&
            isExternal(route.path)
          "
          class="nav-link text-sm block px-1.5 py-0.5 rounded-md relative uppercase cursor-pointer"
          :href="route.path"
          :data-menu="route.name"
        >
          <span class="relative z-50">
            {{ locale ? route.i18n[locale] : route.name }}
          </span>
        </a>
        <RouterLink
          v-else-if="route.children && route.children.length === 0"
          class="nav-link text-sm block px-1.5 py-0.5 rounded-md relative uppercase cursor-pointer"
          :to="localizedPath(route.path, appStore.locale)"
          :data-menu="route.name"
        >
          <span class="relative z-50">
            {{ locale ? route.i18n[locale] : route.name }}
          </span>
        </RouterLink>
        <Dropdown
          hover
          v-else
          class="nav-link text-sm block px-1.5 py-0.5 rounded-md relative uppercase"
        >
          <button type="button" class="nav-trigger relative z-50">
            {{ locale ? route.i18n[locale] : route.name }}
          </button>
          <DropdownMenu>
            <DropdownItem
              v-for="sub in route.children"
              :key="sub.path"
              :href="isExternal(sub.path) ? sub.path : undefined"
              :to="
                isExternal(sub.path)
                  ? undefined
                  : localizedPath(sub.path, appStore.locale)
              "
            >
              <span class="relative z-50">
                {{ locale ? sub.i18n[locale] : sub.name }}
              </span>
            </DropdownItem>
          </DropdownMenu>
        </Dropdown>
      </li>
    </ul>
  </nav>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useAppStore } from '@/stores/app'
import { Dropdown, DropdownMenu, DropdownItem } from '@/components/Dropdown'
import { isExternal } from '@/utils/validate'
import { localizedPath } from '@/router/localePath'

const appStore = useAppStore()

const locale = computed(() => appStore.locale)
const routes = computed(() => appStore.themeConfig.menu.menus)
</script>

<style lang="scss">
.nav-link {
  @apply hover:text-white font-extrabold;
  &:hover {
    &:before {
      @apply opacity-60 bg-gray-800;
    }
  }
  &:before {
    @apply absolute rounded-lg opacity-0 bg-transparent transition z-40;
    content: '';
    top: -4px;
    left: -4px;
    width: calc(100% + 8px);
    height: calc(100% + 8px);
  }
}

.nav-trigger {
  @apply appearance-none border-0 bg-transparent p-0 text-inherit cursor-pointer;
  font: inherit;
}

.header-active {
  .nav-link {
    @apply text-ob-bright;
    &:hover {
      &:before {
        @apply bg-ob-trans;
      }
    }
  }
}
</style>
