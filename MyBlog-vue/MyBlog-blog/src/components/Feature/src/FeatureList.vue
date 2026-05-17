<template>
  <div class="inverted-main-grid py-8 gap-8 box-border">
    <div class="feature-intro-card relative overflow-hidden rounded-2xl bg-ob-deep-800 shadow-lg">
      <div
        class="feature-intro-shell ob-gradient-plate opacity-90 relative z-10 bg-ob-deep-900 rounded-2xl shadow-md">
        <h2 class="feature-intro-title locale-ui locale-sensitive-case">
          <span class="feature-intro-kicker" :style="gradientText">{{ t('home.editor_pick') }}</span>
          <span class="feature-intro-main relative text-ob-bright font-semibold">
            <svg-icon class="inline-block" icon-class="hot" />
            {{ t('home.recommended') }}
          </span>
        </h2>
      </div>
      <span class="absolute top-0 w-full h-full z-0" :style="gradientBackground" />
    </div>

    <ul class="grid lg:grid-cols-2 gap-8">
      <template v-if="featuredArticles.length > 0">
        <li v-for="article in featuredArticles" :key="article.id">
          <ArticleCard class="home-featured-article" :data="article" />
        </li>
      </template>
      <template v-else>
        <li v-for="n in 2" :key="n">
          <ArticleCard :data="{}" />
        </li>
      </template>
    </ul>
  </div>
</template>

<script lang="ts">
// @ts-nocheck
import { useAppStore } from '@/stores/app'
import { useArticleStore } from '@/stores/article'
import { useI18n } from 'vue-i18n'
import { computed, defineComponent, toRef } from 'vue'
import { ArticleCard } from '@/components/ArticleCard'

export default defineComponent({
  name: 'FeatureList',
  components: {
    ArticleCard
  },
  setup() {
    const appStore = useAppStore()
    const articleStore = useArticleStore()
    const { t } = useI18n()
    return {
      gradientBackground: computed(() => {
        return { background: appStore.themeConfig.header_gradient_css }
      }),
      gradientText: appStore.themeConfig.background_gradient_style,
      featuredArticles: toRef(articleStore.$state, 'featuredArticles'),
      t
    }
  }
})
</script>

<style lang="scss">
.feature-intro-card {
  min-height: 19rem;
}

.feature-intro-shell {
  min-height: calc(100% - 0.5rem);
  padding: 2rem;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.feature-intro-title {
  margin: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.85rem;
  line-height: 1.15;
}

.feature-intro-kicker {
  display: block;
  font-size: clamp(2rem, 2.4vw, 2.7rem);
  font-weight: 700;
}

.feature-intro-main {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  font-size: clamp(1.5rem, 2vw, 2rem);
}

html[lang='cn'] .feature-intro-title,
html[lang='jp'] .feature-intro-title {
  letter-spacing: 0;
}

@media (min-width: 1024px) {
  .feature-intro-card {
    min-height: 100%;
  }

  .feature-intro-shell {
    justify-content: flex-start;
    text-align: left;
    padding: 2rem 2rem 2.5rem;
  }

  .feature-intro-title {
    align-items: flex-start;
  }
}

.home-featured-article {
  .article-content {
    p {
      overflow: hidden;
      text-overflow: ellipsis;
      display: -webkit-box;
      -webkit-line-clamp: 4;
      -webkit-box-orient: vertical;
    }
    .article-footer {
      margin-top: 13px;
    }
  }
}
</style>
