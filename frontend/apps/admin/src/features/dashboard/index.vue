<script setup lang="ts">
import { computed, onMounted } from "vue";
import { transformI18n } from "@/plugins/i18n";
import { useUserStoreHook } from "@/store/modules/user";
import { useStatsDashboard } from "./useStatsDashboard";

defineOptions({ name: "Dashboard" });

const userStore = useUserStoreHook();
const user = computed(() => userStore.currentUser);
const displayName = computed(
  () => user.value?.profile.nickname || user.value?.username || "-"
);
const { dashboard, loading, error, isEmpty, refresh, load } = useStatsDashboard();

function languageLabel(language: string): string {
  return transformI18n(`dashboard.languages.${language}`);
}

function ratioLabel(ratio: number): string {
  return `${(ratio * 100).toFixed(1)}%`;
}

onMounted(load);
</script>

<template>
  <section class="dashboard-page">
    <el-alert
      v-if="userStore.isDemo"
      data-testid="demo-read-only"
      type="warning"
      :closable="false"
      :title="transformI18n('status.readOnlyDemo')"
      show-icon
    />

    <el-card class="mt-4">
      <template #header>
        {{ transformI18n("dashboard.welcome") }}, {{ displayName }}
      </template>
      <el-descriptions :column="1" border>
        <el-descriptions-item :label="transformI18n('dashboard.account')">
          {{ user?.username }}
        </el-descriptions-item>
        <el-descriptions-item :label="transformI18n('dashboard.role')">
          {{ user?.type }}
        </el-descriptions-item>
        <el-descriptions-item :label="transformI18n('dashboard.backendStatus')">
          <el-tag type="success">
            {{ transformI18n("dashboard.connected") }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-skeleton
      v-if="loading && !dashboard"
      data-testid="dashboard-loading"
      class="mt-4"
      :rows="6"
      animated
    />

    <el-alert
      v-else-if="error"
      data-testid="dashboard-error"
      class="mt-4"
      type="error"
      :closable="false"
      :title="transformI18n('dashboard.loadError')"
      show-icon
    >
      <el-button data-testid="dashboard-retry" type="primary" link @click="refresh">
        {{ transformI18n("articles.actions.retry") }}
      </el-button>
    </el-alert>

    <el-empty
      v-else-if="isEmpty"
      data-testid="dashboard-empty"
      class="mt-4"
      :description="transformI18n('dashboard.empty')"
    />

    <template v-else-if="dashboard">
      <section class="metric-grid mt-4">
        <el-card data-testid="dashboard-metric-period-pv" shadow="never">
          <p>{{ transformI18n("dashboard.metrics.periodPv") }}</p>
          <strong>{{ dashboard.periodPv }}</strong>
        </el-card>
        <el-card data-testid="dashboard-metric-today-pv" shadow="never">
          <p>{{ transformI18n("dashboard.metrics.todayPv") }}</p>
          <strong>{{ dashboard.todayPv }}</strong>
        </el-card>
        <el-card data-testid="dashboard-metric-today-uv" shadow="never">
          <p>{{ transformI18n("dashboard.metrics.todayUv") }}</p>
          <strong>{{ dashboard.todayUv }}</strong>
        </el-card>
        <el-card data-testid="dashboard-metric-average-daily-uv" shadow="never">
          <p>{{ transformI18n("dashboard.metrics.averageDailyUv") }}</p>
          <strong>{{ dashboard.averageDailyUv }}</strong>
        </el-card>
      </section>

      <section class="dashboard-grid mt-4">
        <el-card shadow="never">
          <template #header>{{ transformI18n("dashboard.trend") }}</template>
          <ul class="stat-list">
            <li
              v-for="point in dashboard.trend"
              :key="point.date"
              :data-testid="`dashboard-trend-${point.date}`"
            >
              <span>{{ point.date }}</span>
              <span>PV {{ point.pv }} / UV {{ point.uv }}</span>
            </li>
          </ul>
        </el-card>

        <el-card shadow="never">
          <template #header>{{ transformI18n("dashboard.topArticles") }}</template>
          <ul class="stat-list">
            <li
              v-for="article in dashboard.topArticles"
              :key="article.articleId"
              :data-testid="`dashboard-top-article-${article.articleId}`"
            >
              <span>{{ article.title || article.articleId }}</span>
              <span>PV {{ article.pv }} / UV {{ article.dailyUvSum }}</span>
            </li>
          </ul>
        </el-card>

        <el-card shadow="never">
          <template #header>{{ transformI18n("dashboard.languageDistribution") }}</template>
          <ul class="stat-list">
            <li
              v-for="item in dashboard.languageDistribution"
              :key="item.language"
              :data-testid="`dashboard-language-${item.language}`"
            >
              <span>{{ languageLabel(item.language) }}</span>
              <span>{{ item.pv }} / {{ ratioLabel(item.ratio) }}</span>
            </li>
          </ul>
        </el-card>
      </section>
    </template>
  </section>
</template>

<style scoped lang="scss">
.dashboard-page {
  padding: 20px;
}

.metric-grid,
.dashboard-grid {
  display: grid;
  gap: 16px;
}

.metric-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));

  p {
    margin: 0 0 8px;
    font-size: 13px;
    color: var(--el-text-color-secondary);
  }

  strong {
    font-size: 26px;
    font-weight: 700;
    color: var(--el-text-color-primary);
  }
}

.dashboard-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.stat-list {
  display: grid;
  gap: 10px;
  padding: 0;
  margin: 0;
  list-style: none;

  li {
    display: flex;
    gap: 12px;
    align-items: center;
    justify-content: space-between;
    padding: 10px 0;
    border-bottom: 1px solid var(--el-border-color-lighter);
  }
}

@media (width <= 1100px) {
  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .dashboard-grid {
    grid-template-columns: 1fr;
  }
}

@media (width <= 700px) {
  .dashboard-page {
    padding: 12px;
  }

  .metric-grid {
    grid-template-columns: 1fr;
  }
}
</style>
