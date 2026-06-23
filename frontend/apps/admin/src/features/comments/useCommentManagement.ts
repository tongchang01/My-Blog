import { reactive, ref } from "vue";
import type { ApiResponse } from "@/api/contract";
import {
  approveComment,
  deleteComment,
  hideComment,
  listComments,
  restoreComment
} from "@/api/comment";
import type {
  CommentListFilters,
  CommentListItem,
  CommentPageResponse
} from "./model";

export interface CommentManagementApi {
  listComments(
    filters: CommentListFilters
  ): Promise<ApiResponse<CommentPageResponse>>;
  approveComment(id: string): Promise<ApiResponse<null>>;
  hideComment(id: string): Promise<ApiResponse<null>>;
  deleteComment(id: string): Promise<ApiResponse<null>>;
  restoreComment(id: string): Promise<ApiResponse<null>>;
}

const defaultApi: CommentManagementApi = {
  listComments,
  approveComment,
  hideComment,
  deleteComment,
  restoreComment
};

const DEFAULT_FILTERS: CommentListFilters = {
  targetType: "ALL",
  targetId: "",
  auditStatus: "ALL",
  keyword: "",
  includeDeleted: false,
  page: 1,
  size: 20
};

function asError(error: unknown): Error {
  return error instanceof Error ? error : new Error(String(error));
}

export function useCommentManagement(api: CommentManagementApi = defaultApi) {
  const filters = reactive<CommentListFilters>({ ...DEFAULT_FILTERS });
  const items = ref<CommentListItem[]>([]);
  const total = ref(0);
  const loading = ref(false);
  const error = ref<Error | null>(null);
  const operationError = ref<Error | null>(null);
  const operatingId = ref<string | null>(null);
  let requestVersion = 0;

  async function loadComments(): Promise<void> {
    const version = ++requestVersion;
    loading.value = true;
    error.value = null;
    const requestFilters = { ...filters };
    try {
      const response = await api.listComments(requestFilters);
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
    await loadComments();
  }

  async function search(): Promise<void> {
    filters.page = 1;
    await loadComments();
  }

  async function reset(): Promise<void> {
    Object.assign(filters, DEFAULT_FILTERS);
    await loadComments();
  }

  async function refresh(): Promise<void> {
    await loadComments();
  }

  async function changePage(page: number, size = filters.size): Promise<void> {
    filters.page = page;
    filters.size = size;
    await loadComments();
  }

  async function refreshAfterOperation(): Promise<void> {
    await loadComments();
    if (
      !error.value &&
      items.value.length === 0 &&
      total.value > 0 &&
      filters.page > 1
    ) {
      filters.page -= 1;
      await loadComments();
    }
  }

  async function runOperation(
    id: string,
    operation: (commentId: string) => Promise<ApiResponse<null>>
  ): Promise<boolean> {
    operationError.value = null;
    operatingId.value = id;
    try {
      await operation(id);
      await refreshAfterOperation();
      return true;
    } catch (reason) {
      operationError.value = asError(reason);
      return false;
    } finally {
      operatingId.value = null;
    }
  }

  return {
    filters,
    items,
    total,
    loading,
    error,
    operationError,
    operatingId,
    initialize,
    search,
    reset,
    refresh,
    changePage,
    approve: (id: string) => runOperation(id, api.approveComment),
    hide: (id: string) => runOperation(id, api.hideComment),
    remove: (id: string) => runOperation(id, api.deleteComment),
    restore: (id: string) => runOperation(id, api.restoreComment)
  };
}
