import { reactive, ref } from "vue";
import type { ApiResponse } from "@/api/contract";
import {
  deleteAttachment,
  listDeletedAttachments,
  listAttachments,
  restoreAttachment,
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
  listDeletedAttachments(
    params: AttachmentListParams
  ): Promise<ApiResponse<AttachmentPageResponse>>;
  uploadAttachment(file: File): Promise<ApiResponse<AttachmentItem>>;
  deleteAttachment(id: string): Promise<ApiResponse<null>>;
  restoreAttachment(id: string): Promise<ApiResponse<null>>;
}

const defaultApi: AttachmentManagementApi = {
  listAttachments,
  listDeletedAttachments,
  uploadAttachment,
  deleteAttachment,
  restoreAttachment
};

export const MAX_ATTACHMENT_UPLOAD_BYTES = 10 * 1024 * 1024;

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
  const operationError = ref<Error | null>(null);
  const showDeleted = ref(false);
  let requestVersion = 0;

  async function loadAttachments(): Promise<void> {
    const version = ++requestVersion;
    loading.value = true;
    error.value = null;
    const requestParams = { ...pagination };
    try {
      const response = showDeleted.value
        ? await api.listDeletedAttachments(requestParams)
        : await api.listAttachments(requestParams);
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
    if (file.size > MAX_ATTACHMENT_UPLOAD_BYTES) {
      uploadError.value = new Error("FILE_TOO_LARGE");
      uploading.value = false;
      return false;
    }
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

  async function showDeletedAttachments(): Promise<void> {
    showDeleted.value = true;
    pagination.page = 1;
    await loadAttachments();
  }

  async function showActiveAttachments(): Promise<void> {
    showDeleted.value = false;
    pagination.page = 1;
    await loadAttachments();
  }

  async function remove(id: string): Promise<boolean> {
    operationError.value = null;
    try {
      await api.deleteAttachment(id);
      await loadAttachments();
      return true;
    } catch (reason) {
      operationError.value = asError(reason);
      return false;
    }
  }

  async function restore(id: string): Promise<boolean> {
    operationError.value = null;
    try {
      await api.restoreAttachment(id);
      await loadAttachments();
      return true;
    } catch (reason) {
      operationError.value = asError(reason);
      return false;
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
    operationError,
    showDeleted,
    initialize,
    refresh,
    changePage,
    upload,
    showDeletedAttachments,
    showActiveAttachments,
    remove,
    restore
  };
}
