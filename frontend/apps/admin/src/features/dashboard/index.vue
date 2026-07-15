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
const user = computed(() => userStore.currentUser);
const displayName = computed(
  () => user.value?.profile.nickname || user.value?.username || "-"
);
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
const topArticlesChartRef = ref<HTMLElement | null>(null);
const languageChartRef = ref<HTMLElement | null>(null);
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

  if (topArticlesChartRef.value && canRenderChart(topArticlesChartRef.value)) {
    chartFor(topArticlesChartRef.value).setOption({
      tooltip: { trigger: "axis" },
      grid: { left: 36, right: 12, top: 12, bottom: 28 },
      xAxis: {
        type: "category",
        data: current.topArticles.map(item => item.title || item.articleId)
      },
      yAxis: { type: "value" },
      series: [
        {
          name: "PV",
          type: "bar",
          data: current.topArticles.map(item => item.pv)
        }
      ]
    });
  }

  if (languageChartRef.value && canRenderChart(languageChartRef.value)) {
    chartFor(languageChartRef.value).setOption({
      tooltip: { trigger: "item" },
      legend: { bottom: 0 },
      series: [
        {
          name: transformI18n("dashboard.languageDistribution"),
          type: "pie",
          radius: ["45%", "70%"],
          data: current.languageDistribution.map(item => ({
            name: languageLabel(item.language),
            value: item.pv
          }))
        }
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

    <el-card class="dashboard-overview mt-4" shadow="never">
      <template #header>
        <div class="dashboard-overview__heading">
          <h1>{{ transformI18n("menus.dashboard") }}</h1>
          <p>{{ transformI18n("dashboard.welcome") }}, {{ displayName }}</p>
        </div>
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

    <el-card data-testid="dashboard-filter" class="mt-4" shadow="never">
      <div class="dashboard-filter">
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
      <p v-if="!filters.from" data-testid="dashboard-default-period">
        {{ transformI18n("dashboard.filter.defaultPeriod") }}
      </p>
      <el-alert
        v-if="filterError"
        data-testid="dashboard-filter-error"
        type="error"
        :closable="false"
        :title="transformI18n(`dashboard.filter.errors.${filterError}`)"
        show-icon
      />
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
      <section class="metric-grid mt-4">
        <el-card data-testid="dashboard-metric-period-pv" shadow="never">
          <el-statistic :value="dashboard.periodPv">
            <template #title>{{ transformI18n("dashboard.metrics.periodPv") }}</template>
          </el-statistic>
        </el-card>
        <el-card data-testid="dashboard-metric-today-pv" shadow="never">
          <el-statistic :value="dashboard.todayPv">
            <template #title>{{ transformI18n("dashboard.metrics.todayPv") }}</template>
          </el-statistic>
        </el-card>
        <el-card data-testid="dashboard-metric-today-uv" shadow="never">
          <el-statistic :value="dashboard.todayUv">
            <template #title>{{ transformI18n("dashboard.metrics.todayUv") }}</template>
          </el-statistic>
        </el-card>
        <el-card data-testid="dashboard-metric-average-daily-uv" shadow="never">
          <el-statistic :value="dashboard.averageDailyUv">
            <template #title>{{ transformI18n("dashboard.metrics.averageDailyUv") }}</template>
          </el-statistic>
        </el-card>
      </section>

      <section class="dashboard-grid mt-4">
        <el-card shadow="never">
          <template #header>{{ transformI18n("dashboard.trend") }}</template>
          <div
            ref="trendChartRef"
            data-testid="dashboard-trend-chart"
            class="dashboard-chart"
          />
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
          <template #header>{{
            transformI18n("dashboard.topArticles")
          }}</template>
          <div
            ref="topArticlesChartRef"
            data-testid="dashboard-top-articles-chart"
            class="dashboard-chart"
          />
          <ul class="stat-list">
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
          </ul>
        </el-card>

        <el-card shadow="never">
          <template #header>{{
            transformI18n("dashboard.languageDistribution")
          }}</template>
          <div
            ref="languageChartRef"
            data-testid="dashboard-language-chart"
            class="dashboard-chart"
          />
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

.dashboard-filter {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.dashboard-filter__actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.dashboard-filter + p {
  margin: 12px 0 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.metric-grid,
.dashboard-grid {
  display: grid;
  gap: 16px;
}

.metric-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));

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

.dashboard-overview__heading {
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
    color: var(--el-text-color-secondary);
  }
}

.dashboard-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.dashboard-chart {
  height: 240px;
  margin-bottom: 14px;
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
