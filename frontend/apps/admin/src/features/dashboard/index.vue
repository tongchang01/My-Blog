<script setup lang="ts">
import {
  computed,
  nextTick,
  onBeforeUnmount,
  onMounted,
  ref,
  watch
} from "vue";
import type { ECharts } from "echarts/core";
import echarts from "@/plugins/echarts";
import { transformI18n } from "@/plugins/i18n";
import { useUserStoreHook } from "@/store/modules/user";
import { lastDaysFilters } from "./query";
import { useStatsDashboard } from "./useStatsDashboard";

defineOptions({ name: "Dashboard" });

const userStore = useUserStoreHook();
const { filters, dashboard, loading, error, filterError, isEmpty, refresh, load } =
  useStatsDashboard();
const dateRange = computed({
  get: () =>
    filters.value.from && filters.value.to
      ? [filters.value.from, filters.value.to]
      : [],
  set: (range: string[] | null) => {
    if (range?.length === 2) {
      [filters.value.from, filters.value.to] = range;
      return;
    }
    filters.value = {};
  }
});
const trendChartRef = ref<HTMLElement | null>(null);
const chartInstances: ECharts[] = [];

function languageLabel(language: string): string {
  return transformI18n(`dashboard.languages.${language}`);
}

function ratioLabel(ratio: number): string {
  return `${(ratio * 100).toFixed(1)}%`;
}

function loadLastDays(days: number): void {
  filters.value = lastDaysFilters(days);
  void load();
}

function resetFilters(): void {
  filters.value = {};
  void load();
}

function chartFor(element: HTMLElement): ECharts {
  const existing = echarts.getInstanceByDom(element);
  if (existing) return existing;
  const chart = echarts.init(element, undefined, { renderer: "svg" });
  chartInstances.push(chart);
  return chart;
}

function canRenderChart(element: HTMLElement): boolean {
  return element.clientWidth > 0 || element.clientHeight > 0;
}

async function renderCharts(): Promise<void> {
  const current = dashboard.value;
  if (!current) return;
  await nextTick();

  if (trendChartRef.value && canRenderChart(trendChartRef.value)) {
    chartFor(trendChartRef.value).setOption({
      tooltip: { trigger: "axis" },
      legend: { top: 0 },
      grid: { left: 36, right: 12, top: 36, bottom: 28 },
      xAxis: { type: "category", data: current.trend.map(item => item.date) },
      yAxis: { type: "value" },
      series: [
        { name: "PV", type: "line", data: current.trend.map(item => item.pv) },
        { name: "UV", type: "line", data: current.trend.map(item => item.uv) }
      ]
    });
  }

}

watch(dashboard, () => {
  void renderCharts();
});

onMounted(load);
onBeforeUnmount(() => {
  chartInstances.splice(0).forEach(chart => chart.dispose());
});
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

    <header class="dashboard-heading">
      <div>
        <h1>{{ transformI18n("menus.dashboard") }}</h1>
        <p v-if="!filters.from" data-testid="dashboard-default-period">
          {{ transformI18n("dashboard.filter.defaultPeriod") }}
        </p>
      </div>
      <div data-testid="dashboard-filter" class="dashboard-filter">
        <el-date-picker
          v-model="dateRange"
          data-testid="dashboard-date-range"
          type="daterange"
          value-format="YYYY-MM-DD"
          clearable
        />
        <div class="dashboard-filter__actions">
          <el-button data-testid="dashboard-last-7" @click="loadLastDays(7)">
            {{ transformI18n("dashboard.filter.last7") }}
          </el-button>
          <el-button data-testid="dashboard-last-30" @click="loadLastDays(30)">
            {{ transformI18n("dashboard.filter.last30") }}
          </el-button>
          <el-button type="primary" @click="load">
            {{ transformI18n("articles.actions.search") }}
          </el-button>
          <el-button @click="resetFilters">
            {{ transformI18n("articles.actions.reset") }}
          </el-button>
        </div>
      </div>
    </header>
    <el-alert
      v-if="filterError"
      data-testid="dashboard-filter-error"
      class="dashboard-filter-error"
      type="error"
      :closable="false"
      :title="transformI18n(`dashboard.filter.errors.${filterError}`)"
      show-icon
    />

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
      <el-button
        data-testid="dashboard-retry"
        type="primary"
        link
        @click="refresh"
      >
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
      <section class="metric-grid">
        <el-card
          data-testid="dashboard-metric-period-pv"
          class="metric-card"
          shadow="never"
        >
          <el-statistic :value="dashboard.periodPv">
            <template #title>{{ transformI18n("dashboard.metrics.periodPv") }}</template>
          </el-statistic>
        </el-card>
        <el-card
          data-testid="dashboard-metric-today-pv"
          class="metric-card"
          shadow="never"
        >
          <el-statistic :value="dashboard.todayPv">
            <template #title>{{ transformI18n("dashboard.metrics.todayPv") }}</template>
          </el-statistic>
        </el-card>
        <el-card
          data-testid="dashboard-metric-today-uv"
          class="metric-card"
          shadow="never"
        >
          <el-statistic :value="dashboard.todayUv">
            <template #title>{{ transformI18n("dashboard.metrics.todayUv") }}</template>
          </el-statistic>
        </el-card>
      </section>

      <section class="dashboard-main-grid">
        <el-card class="dashboard-card" shadow="never">
          <template #header>{{ transformI18n("dashboard.trend") }}</template>
          <div
            ref="trendChartRef"
            data-testid="dashboard-trend-chart"
            class="dashboard-chart"
          />
        </el-card>

        <el-card class="dashboard-card" shadow="never">
          <template #header>{{ transformI18n("dashboard.topArticles") }}</template>
          <ol class="article-ranking">
            <li
              v-for="article in dashboard.topArticles"
              :key="article.articleId"
              :data-testid="`dashboard-top-article-${article.articleId}`"
            >
              <span>{{ article.title || article.articleId }}</span>
              <span>
                PV {{ article.pv }} /
                {{ transformI18n("dashboard.dailyUvSum") }}
                {{ article.dailyUvSum }}
              </span>
            </li>
          </ol>
        </el-card>
      </section>

      <el-card class="dashboard-card language-card" shadow="never">
        <template #header>{{ transformI18n("dashboard.languageDistribution") }}</template>
        <ul class="language-list">
            <li
              v-for="item in dashboard.languageDistribution"
              :key="item.language"
              :data-testid="`dashboard-language-${item.language}`"
            >
              <div class="language-list__heading">
                <span>{{ languageLabel(item.language) }}</span>
                <span>{{ item.pv }} / {{ ratioLabel(item.ratio) }}</span>
              </div>
              <div class="language-list__bar" aria-hidden="true">
                <span :style="{ width: ratioLabel(item.ratio) }" />
              </div>
            </li>
        </ul>
      </el-card>
    </template>
  </section>
</template>

<style scoped lang="scss">
.dashboard-page {
  padding: 20px 24px;
}

.dashboard-heading,
.dashboard-filter {
  display: flex;
  align-items: center;
}

.dashboard-heading {
  justify-content: space-between;
  gap: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--el-border-color-lighter);

  h1,
  p {
    margin: 0;
  }

  h1 {
    font-size: 20px;
    line-height: 28px;
  }

  p {
    margin-top: 4px;
    font-size: 13px;
    color: var(--el-text-color-secondary);
  }
}

.dashboard-filter {
  justify-content: flex-end;
  gap: 12px;
  flex-wrap: wrap;
}

.dashboard-filter__actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.dashboard-filter-error {
  margin-top: 16px;
}

.metric-grid,
.dashboard-main-grid {
  display: grid;
  gap: 16px;
}

.metric-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin-top: 20px;

  .metric-card {
    border-color: var(--el-border-color-lighter);
  }

  :deep(.el-statistic__head) {
    margin-bottom: 8px;
    font-size: 13px;
    color: var(--el-text-color-secondary);
  }

  :deep(.el-statistic__content) {
    font-size: 26px;
    font-weight: 700;
    color: var(--el-text-color-primary);
  }
}

.dashboard-main-grid {
  grid-template-columns: minmax(0, 2fr) minmax(280px, 1fr);
  margin-top: 16px;
}

.dashboard-card {
  border-color: var(--el-border-color-lighter);
}

.dashboard-chart {
  height: 286px;
}

.article-ranking,
.language-list {
  display: grid;
  padding: 0;
  margin: 0;
  list-style: none;
}

.article-ranking {
  counter-reset: article-rank;

  li {
    display: flex;
    gap: 12px;
    align-items: center;
    justify-content: space-between;
    min-height: 48px;
    border-bottom: 1px solid var(--el-border-color-lighter);

    &::before {
      min-width: 20px;
      font-weight: 600;
      color: var(--el-text-color-secondary);
      content: counter(article-rank);
      counter-increment: article-rank;
    }

    > span:first-of-type {
      flex: 1;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    > span:last-of-type {
      font-size: 13px;
      color: var(--el-text-color-secondary);
      white-space: nowrap;
    }
  }
}

.language-card {
  margin-top: 16px;
}

.language-list {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;

  li {
    min-width: 0;
  }
}

.language-list__heading {
  display: flex;
  gap: 12px;
  justify-content: space-between;
  margin-bottom: 10px;
  font-size: 13px;

  span:last-child {
    color: var(--el-color-primary);
    white-space: nowrap;
  }
}

.language-list__bar {
  height: 6px;
  overflow: hidden;
  background: var(--el-fill-color-light);
  border-radius: 999px;

  span {
    display: block;
    height: 100%;
    background: var(--el-color-primary);
    border-radius: inherit;
  }
}

@media (width <= 1100px) {
  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .dashboard-main-grid {
    grid-template-columns: 1fr;
  }

  .language-list {
    grid-template-columns: 1fr;
    gap: 16px;
  }
}

@media (width <= 700px) {
  .dashboard-page {
    padding: 12px;
  }

  .dashboard-heading {
    align-items: flex-start;
    flex-direction: column;
  }

  .dashboard-filter {
    justify-content: flex-start;
  }

  .dashboard-filter :deep(.el-date-editor) {
    width: 100%;
  }

  .metric-grid {
    grid-template-columns: 1fr;
  }
}
</style>
