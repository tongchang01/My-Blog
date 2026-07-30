import { ref } from "vue";
import { getStatsDashboard } from "@/api/stats";
import type { StatsDashboard, StatsDashboardFilters } from "./model";
import {
  type StatsDashboardFilterError,
  validateStatsDashboardFilters
} from "./query";

export function useStatsDashboard(initialFilters: StatsDashboardFilters = {}) {
  const filters = ref<StatsDashboardFilters>({ ...initialFilters });
  const dashboard = ref<StatsDashboard | null>(null);
  const loading = ref(false);
  const error = ref(false);
  const filterError = ref<StatsDashboardFilterError | null>(null);

  async function load(): Promise<void> {
    filterError.value = validateStatsDashboardFilters(filters.value);
    if (filterError.value) return;
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
    filterError,
    load,
    refresh: load
  };
}
