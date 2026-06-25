import { reactive, ref } from "vue";
import type { ApiResponse } from "@/api/contract";
import {
  listAttachments,
  uploadAttachment
} from "@/api/attachment";
import type {
  AttachmentItem,
  AttachmentListParams,
  AttachmentPageResponse
} from "./model";

export interface AttachmentManagementApi {
  listAttachments(
    params: AttachmentListParams
  ): Promise<ApiResponse<AttachmentPageResponse>>;
  uploadAttachment(file: File): Promise<ApiResponse<AttachmentItem>>;
}

const defaultApi: AttachmentManagementApi = {
  listAttachments,
  uploadAttachment
};

function asError(error: unknown): Error {
  return error instanceof Error ? error : new Error(String(error));
}

export function useAttachmentManagement(
  api: AttachmentManagementApi = defaultApi
) {
  const pagination = reactive<AttachmentListParams>({
    page: 1,
    size: 20
  });
  const items = ref<AttachmentItem[]>([]);
  const total = ref(0);
  const loading = ref(false);
  const uploading = ref(false);
  const error = ref<Error | null>(null);
  const uploadError = ref<Error | null>(null);
  let requestVersion = 0;

  async function loadAttachments(): Promise<void> {
    const version = ++requestVersion;
    loading.value = true;
    error.value = null;
    const requestParams = { ...pagination };
    try {
      const response = await api.listAttachments(requestParams);
      if (version !== requestVersion) return;
      items.value = response.data.records;
      total.value = response.data.total;
    } catch (reason) {
      if (version !== requestVersion) return;
      error.value = asError(reason);
    } finally {
      if (version === requestVersion) loading.value = false;
    }
  }

  async function initialize(): Promise<void> {
    await loadAttachments();
  }

  async function refresh(): Promise<void> {
    await loadAttachments();
  }

  async function changePage(
    page: number,
    size = pagination.size
  ): Promise<void> {
    pagination.page = page;
    pagination.size = size;
    await loadAttachments();
  }

  async function upload(file: File): Promise<boolean> {
    uploading.value = true;
    uploadError.value = null;
    try {
      await api.uploadAttachment(file);
      pagination.page = 1;
      await loadAttachments();
      return true;
    } catch (reason) {
      uploadError.value = asError(reason);
      return false;
    } finally {
      uploading.value = false;
    }
  }

  return {
    pagination,
    items,
    total,
    loading,
    uploading,
    error,
    uploadError,
    initialize,
    refresh,
    changePage,
    upload
  };
}
