import type { ApiResponse } from "./contract";
import type {
  SiteConfig,
  SiteConfigPayload
} from "@/features/site-config/model";
import { http } from "@/utils/http";

export const getSiteConfig = () =>
  http.get<ApiResponse<SiteConfig>>("/api/admin/site-config");

export const updateSiteConfig = (payload: SiteConfigPayload) =>
  http.request<ApiResponse<SiteConfig>>("put", "/api/admin/site-config", {
    data: payload
  });
