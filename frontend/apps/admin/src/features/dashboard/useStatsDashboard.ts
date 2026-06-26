import { computed, ref } from "vue";
import { getStatsDashboard } from "@/api/stats";
import type {
  StatsDashboard,
  StatsDashboardFilters
} from "./model";

export function useStatsDashboard(initialFilters: StatsDashboardFilters = {}) {
  const filters = ref<StatsDashboardFilters>({ ...initialFilters });
  const dashboard = ref<StatsDashboard | null>(null);
  const loading = ref(false);
  const error = ref(false);

  const isEmpty = computed(() => {
    const current = dashboard.value;
    if (!current) return false;
    return (
      current.periodPv === 0 &&
      current.todayPv === 0 &&
      current.todayUv === 0 &&
      current.trend.length === 0 &&
      current.topArticles.length === 0 &&
      current.languageDistribution.length === 0
    );
  });

  async function load(): Promise<void> {
    loading.value = true;
    error.value = false;
    try {
      const response = await getStatsDashboard(filters.value);
      dashboard.value = response.data;
    } catch {
      error.value = true;
    } finally {
      loading.value = false;
    }
  }

  return {
    filters,
    dashboard,
    loading,
    error,
    isEmpty,
    load,
    refresh: load
  };
}
