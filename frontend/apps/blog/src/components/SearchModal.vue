<template>
  <div
    v-if="openModal"
    id="search-modal"
    @keydown.esc="handleStatusChange(false)"
    @keydown.meta.k.stop.prevent="handleStatusChange(false)"
    @keydown.arrow-up.stop.prevent="handleArrowUp"
    @keydown.arrow-down.stop.prevent="handleArrowDown"
    @keydown.enter.stop.prevent="handleEnterDown"
    @click.self="handleStatusChange(false)"
    tabindex="-1"
  >
    <transition name="fade-bounce-pure-y" mode="out-in">
      <div
        id="search-container"
        class="search-container"
        v-if="openSearchContainer"
      >
        <header class="flex pt-4 pr-4 pl-4">
          <form class="search-form" action="">
            <label
              id="search-label"
              class="items-center flex justify-center"
              for="search-input"
            >
              <svg
                class="text-ob fill-current stroke-current"
                width="32"
                height="32"
                viewBox="0 0 24 24"
                fill="none"
                xmlns="http://www.w3.org/2000/svg"
                data-reactroot=""
              >
                <path
                  stroke-linejoin="round"
                  stroke-linecap="round"
                  stroke-width="1"
                  stroke=""
                  d="M15.9996 15.2877L15.2925 15.9948L21.2958 21.9981L22.0029 21.291L15.9996 15.2877Z"
                ></path>
                <path
                  stroke-linejoin="round"
                  stroke-linecap="round"
                  stroke-width="1"
                  stroke=""
                  fill="rgba(0,0,0,0)"
                  d="M10 18C14.4183 18 18 14.4183 18 10C18 5.58172 14.4183 2 10 2C5.58172 2 2 5.58172 2 10C2 14.4183 5.58172 18 10 18Z"
                ></path>
              </svg>
            </label>
            <input
              type="search"
              id="search-input"
              ref="searchInput"
              class="search-input"
              autocomplete="off"
              v-model="keyword"
              @input="searchKeyword"
            />
            <button
              v-show="keyword.length > 0"
              class="search-btn"
              type="reset"
              title="Clear the query"
              @click="handleResetInput"
            >
              <svg width="20" height="20" viewBox="0 0 20 20">
                <path
                  d="M10 10l5.09-5.09L10 10l5.09 5.09L10 10zm0 0L4.91 4.91 10 10l-5.09 5.09L10 10z"
                  stroke="currentColor"
                  fill="none"
                  fill-rule="evenodd"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                ></path>
              </svg>
            </button>
          </form>
        </header>
        <div
          id="Search-Dropdown"
          class="search-dropdown"
          v-if="
            (searchResults.length > 0 || recentResults.length > 0) && !isEmpty
          "
        >
          <div>
            <section v-if="searchResults.length > 0">
              <div class="search-hit-label">
                {{ searchResultsCount }}
              </div>
              <ul id="search-menu">
                <li
                  v-for="(result, index) in searchResults"
                  :key="result.slug"
                  :class="{
                    'search-hit': true,
                    active: index == menuActiveIndex
                  }"
                  :id="'search-hit-item-' + index"
                >
                  <a href="javascript:void(0)" @click="handleLinkClick(result)">
                    <div class="search-hit-container">
                      <div class="search-hit-icon">
                        <svg width="20" height="20" viewBox="0 0 20 20">
                          <path
                            d="M17 6v12c0 .52-.2 1-1 1H4c-.7 0-1-.33-1-1V2c0-.55.42-1 1-1h8l5 5zM14 8h-3.13c-.51 0-.87-.34-.87-.87V4"
                            stroke="currentColor"
                            fill="none"
                            fill-rule="evenodd"
                            stroke-linejoin="round"
                          ></path>
                        </svg>
                      </div>
                      <div class="search-hit-content-wrapper">
                        <span class="search-hit-title">
                          {{ result.content }}
                        </span>
                        <span class="search-hit-path">{{ result.title }}</span>
                      </div>
                      <div class="search-hit-action">
                        <svg
                          class="DocSearch-Hit-Select-Icon"
                          width="20"
                          height="20"
                          viewBox="0 0 20 20"
                        >
                          <g
                            stroke="currentColor"
                            fill="none"
                            fill-rule="evenodd"
                            stroke-linecap="round"
                            stroke-linejoin="round"
                          >
                            <path d="M18 3v4c0 2-2 4-4 4H2"></path>
                            <path d="M8 17l-6-6 6-6"></path>
                          </g>
                        </svg>
                      </div>
                    </div>
                  </a>
                </li>
              </ul>
            </section>
            <section v-else>
              <div class="search-hit-label">
                {{ t('settings.recently-search') }}
              </div>
              <ul id="search-menu">
                <li
                  v-for="(result, index) in recentResults"
                  :key="result.slug"
                  :class="{
                    'search-hit': true,
                    active: index == menuActiveIndex
                  }"
                  :id="'search-hit-item-' + index"
                >
                  <a href="javascript:void(0)" @click="handleLinkClick(result)">
                    <div class="search-hit-container">
                      <div class="search-hit-icon">
                        <svg width="20" height="20" viewBox="0 0 20 20">
                          <path
                            d="M17 6v12c0 .52-.2 1-1 1H4c-.7 0-1-.33-1-1V2c0-.55.42-1 1-1h8l5 5zM14 8h-3.13c-.51 0-.87-.34-.87-.87V4"
                            stroke="currentColor"
                            fill="none"
                            fill-rule="evenodd"
                            stroke-linejoin="round"
                          ></path>
                        </svg>
                      </div>
                      <div class="search-hit-content-wrapper">
                        <span class="search-hit-title">
                          {{ result.content }}
                        </span>
                        <span class="search-hit-path">{{ result.title }}</span>
                      </div>
                      <div class="search-hit-action">
                        <svg
                          class="DocSearch-Hit-Select-Icon"
                          width="20"
                          height="20"
                          viewBox="0 0 20 20"
                        >
                          <g
                            stroke="currentColor"
                            fill="none"
                            fill-rule="evenodd"
                            stroke-linecap="round"
                            stroke-linejoin="round"
                          >
                            <path d="M18 3v4c0 2-2 4-4 4H2"></path>
                            <path d="M8 17l-6-6 6-6"></path>
                          </g>
                        </svg>
                      </div>
                    </div>
                  </a>
                </li>
              </ul>
            </section>
          </div>
        </div>
        <div class="search-startscreen" v-else-if="!isEmpty">
          <p>{{ t('settings.no-recent-search') }}</p>
        </div>
        <div class="search-startscreen" v-else>
          <p>{{ t('settings.no-search-result') }}</p>
        </div>
        <div class="search-footer">
          <ul class="search-commands">
            <li>
              <span class="search-commands-key">
                <svg width="15" height="15">
                  <g
                    fill="none"
                    stroke="currentColor"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    stroke-width="1.2"
                  >
                    <path
                      d="M12 3.53088v3c0 1-1 2-2 2H4M7 11.53088l-3-3 3-3"
                    ></path>
                  </g>
                </svg>
              </span>
              <span class="search-commands-label">
                {{ t('settings.cmd-to-select') }}
              </span>
            </li>
            <li>
              <span class="search-commands-key">
                <svg width="15" height="15">
                  <g
                    fill="none"
                    stroke="currentColor"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    stroke-width="1.2"
                  >
                    <path d="M7.5 3.5v8M10.5 8.5l-3 3-3-3"></path>
                  </g>
                </svg>
              </span>
              <span class="search-commands-key">
                <svg width="15" height="15">
                  <g
                    fill="none"
                    stroke="currentColor"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    stroke-width="1.2"
                  >
                    <path d="M7.5 11.5v-8M10.5 6.5l-3-3-3 3"></path>
                  </g>
                </svg>
              </span>
              <span class="search-commands-label">
                {{ t('settings.cmd-to-navigate') }}
              </span>
            </li>
            <li>
              <span class="search-commands-key">
                <svg width="15" height="15">
                  <g
                    fill="none"
                    stroke="currentColor"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    stroke-width="1.2"
                  >
                    <path
                      d="M13.6167 8.936c-.1065.3583-.6883.962-1.4875.962-.7993 0-1.653-.9165-1.653-2.1258v-.5678c0-1.2548.7896-2.1016 1.653-2.1016.8634 0 1.3601.4778 1.4875 1.0724M9 6c-.1352-.4735-.7506-.9219-1.46-.8972-.7092.0246-1.344.57-1.344 1.2166s.4198.8812 1.3445.9805C8.465 7.3992 8.968 7.9337 9 8.5c.032.5663-.454 1.398-1.4595 1.398C6.6593 9.898 6 9 5.963 8.4851m-1.4748.5368c-.2635.5941-.8099.876-1.5443.876s-1.7073-.6248-1.7073-2.204v-.4603c0-1.0416.721-2.131 1.7073-2.131.9864 0 1.6425 1.031 1.5443 2.2492h-2.956"
                    ></path>
                  </g>
                </svg>
              </span>
              <span class="search-commands-label">
                {{ t('settings.cmd-to-close') }}
              </span>
            </li>
          </ul>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import type { ArticleCardViewModel } from '@/features/articles/model'
import { SearchResultType } from '@/models/Search.class'
import { useAppStore } from '@/stores/app'
import { useSearchStore } from '@/stores/search'
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'

/**
 * Lodash package is imported through CDN.
 *
 * For version 4.17.21
 */
declare const _: any

const searchStore = useSearchStore()
const searchInput = ref<HTMLInputElement>()
const searchResults = ref<SearchResultType[]>([])
const appStore = useAppStore()
const router = useRouter()
const openModal = ref(false)
const openSearchContainer = ref(false)
const keyword = ref('')
const recentResults = ref<SearchResultType[]>([])
const menuActiveIndex = ref(0)
const menuMaxIndex = ref(0)
const isEmpty = ref(false)
const { t } = useI18n()

const toSearchResult = (article: ArticleCardViewModel): SearchResultType => ({
  id: article.id,
  title: article.title,
  content: article.summary || article.title,
  slug: article.slug
})

/**
 * Handlers
 */
const handleStatusChange = (status: boolean) => {
  appStore.changeOpenModal(status)
}

const handleLinkClick = (result: SearchResultType) => {
  handleStatusChange(false)
  if (!result.id) return
  searchStore.addRecentSearch(result)
  reloadRecentResult()
  router.push({
    name: 'article-detail',
    params: {
      lang: appStore.locale,
      id: result.id,
      slug: result.slug
    }
  })
}

const handleResetInput = () => {
  keyword.value = ''
  searchResults.value = []
  isEmpty.value = false
  resetIndex(recentResults.value.length)
}

const handleArrowUp = () => {
  if (isEmpty.value === true) return
  if (menuActiveIndex.value === 0) {
    menuActiveIndex.value = menuMaxIndex.value
  } else {
    menuActiveIndex.value = menuActiveIndex.value - 1
  }
  moveMenuWindow()
}

const handleArrowDown = () => {
  if (isEmpty.value === true) return
  if (menuActiveIndex.value === menuMaxIndex.value) {
    menuActiveIndex.value = 0
  } else {
    menuActiveIndex.value = menuActiveIndex.value + 1
  }
  moveMenuWindow()
}

const moveMenuWindow = () => {
  const searchDropdownEl = document.getElementById('Search-Dropdown')
  const activeMenuEl = document.getElementById(
    `search-hit-item-${menuActiveIndex.value}`
  )
  const containerHeight = searchDropdownEl?.getBoundingClientRect().height
  const menuElHeight = activeMenuEl?.getBoundingClientRect().height

  if (menuElHeight && containerHeight && searchDropdownEl) {
    const currentMenuElheight = 36 + menuElHeight * (menuActiveIndex.value + 1)
    const heightDiff = currentMenuElheight - containerHeight
    if (heightDiff > 0) {
      searchDropdownEl.scrollTo({
        top: heightDiff
      })
    }
  }

  if (searchDropdownEl && menuActiveIndex.value === 0) {
    searchDropdownEl.scrollTo({
      top: 0
    })
  }
}

const handleEnterDown = () => {
  if (searchResults.value.length === 0 && recentResults.value.length > 0) {
    handleLinkClick(recentResults.value[menuActiveIndex.value])
  }
  if (searchResults.value.length > 0) {
    handleLinkClick(searchResults.value[menuActiveIndex.value])
  }
}

/**
 * Search for records, debounce this function for 500ms
 * for user to finish typing. Prevent uncessary searches
 * between typing of a keyword.
 */
const searchKeyword = _.debounce(async (event: Event) => {
  const target = event.target as HTMLInputElement | null
  const query = target?.value.trim() ?? ''
  if (query !== '') {
    await searchStore.searchArticles(query, appStore.locale)
    if (query !== keyword.value.trim()) return
    searchResults.value = searchStore.searchResults.map(toSearchResult)
    isEmpty.value = searchResults.value.length === 0
    resetIndex(searchResults.value.length)
  } else {
    isEmpty.value = false
    searchResults.value = []
    resetIndex(recentResults.value.length)
  }
}, 500)

/** Refetch the data of recent search results. */
const reloadRecentResult = () => {
  recentResults.value = searchStore.recentResults.getData()
  resetIndex(recentResults.value.length)
}

const resetIndex = (max: number) => {
  menuActiveIndex.value = 0
  menuMaxIndex.value = max - 1
}

onMounted(() =>
  /** Delay focus for animation to finish. */
  setTimeout(() => {
    if (searchInput.value) searchInput.value.focus()
  }, 200)
)

onUnmounted(() => {
  document.body.classList.remove('modal--active')
})

watch(
  () => appStore.openSearchModal,
  status => {
    searchStore.setOpenModal(status)

    /**
     * This watch is used to delay the animation
     * of the search box container.
     */
    if (status === true) {
      keyword.value = ''
      searchResults.value = []
      isEmpty.value = false
      reloadRecentResult()
      setTimeout(() => {
        if (searchInput.value) searchInput.value.focus()
      }, 200)
    }

    if (status === false) {
      setTimeout(() => {
        openModal.value = status
      }, 850)
    } else {
      openModal.value = status
    }

    setTimeout(() => {
      openSearchContainer.value = status
    }, 200)
  }
)

const searchResultsCount = computed(() => {
  return t('settings.search-result').replace(
    '[total]',
    String(searchResults.value.length)
  )
})
</script>
