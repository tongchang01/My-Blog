import { reactive, ref } from "vue";
import type { ApiResponse } from "@/api/contract";
import { getSiteConfig, updateSiteConfig } from "@/api/site-config";
import {
  createSiteConfigForm,
  siteConfigFormToPayload,
  siteConfigToForm,
  validateSiteConfigForm,
  type SiteConfigForm,
  type SiteConfigFormErrors
} from "./form";
import type { SiteConfig, SiteConfigPayload } from "./model";

export interface SiteConfigManagementApi {
  getSiteConfig(): Promise<ApiResponse<SiteConfig>>;
  updateSiteConfig(
    payload: SiteConfigPayload
  ): Promise<ApiResponse<SiteConfig>>;
}

const defaultApi: SiteConfigManagementApi = {
  getSiteConfig,
  updateSiteConfig
};

function asError(error: unknown): Error {
  return error instanceof Error ? error : new Error(String(error));
}

function clearRecord(record: Record<string, unknown>): void {
  Object.keys(record).forEach(key => delete record[key]);
}

export function useSiteConfigManagement(
  api: SiteConfigManagementApi = defaultApi
) {
  const current = ref<SiteConfig | null>(null);
  const form = reactive<SiteConfigForm>(createSiteConfigForm());
  const formErrors = reactive<SiteConfigFormErrors>({});
  const loading = ref(false);
  const saving = ref(false);
  const error = ref<Error | null>(null);
  const saveError = ref<Error | null>(null);

  function resetForm(config: SiteConfig): void {
    Object.assign(form, siteConfigToForm(config));
    clearRecord(formErrors);
  }

  async function initialize(): Promise<void> {
    loading.value = true;
    error.value = null;
    try {
      const response = await api.getSiteConfig();
      current.value = response.data;
      resetForm(response.data);
    } catch (reason) {
      error.value = asError(reason);
    } finally {
      loading.value = false;
    }
  }

  async function refresh(): Promise<void> {
    await initialize();
  }

  async function save(): Promise<boolean> {
    const errors = validateSiteConfigForm(form);
    clearRecord(formErrors);
    Object.assign(formErrors, errors);
    if (Object.keys(errors).length) return false;

    saving.value = true;
    saveError.value = null;
    try {
      const response = await api.updateSiteConfig(
        siteConfigFormToPayload(form)
      );
      current.value = response.data;
      resetForm(response.data);
      return true;
    } catch (reason) {
      saveError.value = asError(reason);
      return false;
    } finally {
      saving.value = false;
    }
  }

  return {
    current,
    form,
    formErrors,
    loading,
    saving,
    error,
    saveError,
    initialize,
    refresh,
    save
  };
}
