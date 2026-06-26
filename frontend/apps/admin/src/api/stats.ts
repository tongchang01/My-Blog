import type { ApiResponse } from "./contract";
import type {
  StatsDashboard,
  StatsDashboardFilters
} from "@/features/dashboard/model";
import { http } from "@/utils/http";

export const getStatsDashboard = (filters: StatsDashboardFilters = {}) =>
  http.get<ApiResponse<StatsDashboard>>("/api/admin/stats/dashboard", {
    params: {
      from: filters.from,
      to: filters.to
    }
  });
