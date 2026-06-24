import { computed, reactive, ref } from "vue";
import type { ApiResponse } from "@/api/contract";
import {
  createFriendLink,
  deleteFriendLink,
  listFriendLinks,
  updateFriendLink,
  updateFriendLinkSortOrders,
  updateFriendLinkStatus
} from "@/api/friend-link";
import { ApiClientError, type ApiErrorKind } from "@/utils/http/error";
import {
  createFriendLinkForm,
  friendLinkFormToPayload,
  friendLinkToForm,
  validateFriendLinkForm,
  type FriendLinkForm,
  type FriendLinkFormErrors
} from "./form";
import type {
  FriendLinkItem,
  FriendLinkListFilters,
  FriendLinkPageResponse,
  FriendLinkSortItem,
  FriendLinkStatus,
  FriendLinkWritePayload
} from "./model";

export interface FriendLinkManagementApi {
  listFriendLinks(
    filters: FriendLinkListFilters
  ): Promise<ApiResponse<FriendLinkPageResponse>>;
  createFriendLink(
    payload: FriendLinkWritePayload
  ): Promise<ApiResponse<FriendLinkItem>>;
  updateFriendLink(
    id: string,
    payload: FriendLinkWritePayload
  ): Promise<ApiResponse<FriendLinkItem>>;
  updateFriendLinkStatus(
    id: string,
    status: FriendLinkStatus
  ): Promise<ApiResponse<FriendLinkItem>>;
  updateFriendLinkSortOrders(
    items: FriendLinkSortItem[]
  ): Promise<ApiResponse<FriendLinkItem[]>>;
  deleteFriendLink(id: string): Promise<ApiResponse<null>>;
}

const defaultApi: FriendLinkManagementApi = {
  listFriendLinks,
  createFriendLink,
  updateFriendLink,
  updateFriendLinkStatus,
  updateFriendLinkSortOrders,
  deleteFriendLink
};

const DEFAULT_FILTERS: FriendLinkListFilters = {
  keyword: "",
  status: "ALL",
  page: 1,
  size: 20
};

export interface FriendLinkOperationError {
  action: "save" | "status" | "sort" | "delete";
  kind: ApiErrorKind;
}

function asError(error: unknown): Error {
  return error instanceof Error ? error : new Error(String(error));
}

function errorKind(reason: unknown): ApiErrorKind {
  return reason instanceof ApiClientError ? reason.kind : "unknown";
}

function clearRecord(record: Record<string, unknown>): void {
  Object.keys(record).forEach(key => delete record[key]);
}

export function useFriendLinkManagement(
  api: FriendLinkManagementApi = defaultApi
) {
  const filters = reactive<FriendLinkListFilters>({ ...DEFAULT_FILTERS });
  const items = ref<FriendLinkItem[]>([]);
  const total = ref(0);
  const loading = ref(false);
  const error = ref<Error | null>(null);
  const operationError = ref<FriendLinkOperationError | null>(null);
  const operatingId = ref<string | null>(null);
  const dialogOpen = ref(false);
  const editingId = ref<string | null>(null);
  const form = reactive<FriendLinkForm>(createFriendLinkForm());
  const formErrors = reactive<FriendLinkFormErrors>({});
  const saving = ref(false);
  const sortDrafts = reactive<Record<string, number>>({});
  let requestVersion = 0;

  const dirtySortItems = computed<FriendLinkSortItem[]>(() =>
    items.value
      .filter(item => sortDrafts[item.id] !== item.sortOrder)
      .map(item => ({ id: item.id, sortOrder: sortDrafts[item.id] }))
  );

  async function loadFriendLinks(): Promise<void> {
    const version = ++requestVersion;
    loading.value = true;
    error.value = null;
    const requestFilters = { ...filters };
    try {
      const response = await api.listFriendLinks(requestFilters);
      if (version !== requestVersion) return;
      items.value = response.data.records;
      total.value = response.data.total;
      clearRecord(sortDrafts);
      items.value.forEach(item => {
        sortDrafts[item.id] = item.sortOrder;
      });
    } catch (reason) {
      if (version !== requestVersion) return;
      error.value = asError(reason);
    } finally {
      if (version === requestVersion) loading.value = false;
    }
  }

  async function initialize(): Promise<void> {
    await loadFriendLinks();
  }

  async function search(): Promise<void> {
    filters.page = 1;
    await loadFriendLinks();
  }

  async function reset(): Promise<void> {
    Object.assign(filters, DEFAULT_FILTERS);
    await loadFriendLinks();
  }

  async function refresh(): Promise<void> {
    await loadFriendLinks();
  }

  async function changePage(page: number, size = filters.size): Promise<void> {
    filters.page = page;
    filters.size = size;
    await loadFriendLinks();
  }

  async function refreshAfterOperation(): Promise<void> {
    await loadFriendLinks();
    if (
      !error.value &&
      items.value.length === 0 &&
      total.value > 0 &&
      filters.page > 1
    ) {
      filters.page -= 1;
      await loadFriendLinks();
    }
  }

  function resetForm(next: FriendLinkForm): void {
    Object.assign(form, next);
    clearRecord(formErrors);
    operationError.value = null;
  }

  function openCreate(): void {
    editingId.value = null;
    resetForm(createFriendLinkForm());
    dialogOpen.value = true;
  }

  function openEdit(item: FriendLinkItem): void {
    editingId.value = item.id;
    resetForm(friendLinkToForm(item));
    dialogOpen.value = true;
  }

  function closeDialog(): void {
    dialogOpen.value = false;
  }

  async function save(): Promise<boolean> {
    const errors = validateFriendLinkForm(form);
    clearRecord(formErrors);
    Object.assign(formErrors, errors);
    if (Object.keys(errors).length) return false;
    saving.value = true;
    operationError.value = null;
    try {
      const payload = friendLinkFormToPayload(form);
      if (editingId.value) {
        await api.updateFriendLink(editingId.value, payload);
      } else {
        await api.createFriendLink(payload);
      }
      dialogOpen.value = false;
      await refreshAfterOperation();
      return true;
    } catch (reason) {
      operationError.value = { action: "save", kind: errorKind(reason) };
      return false;
    } finally {
      saving.value = false;
    }
  }

  function setSortOrder(id: string, sortOrder: number): void {
    sortDrafts[id] = sortOrder;
  }

  async function saveSortOrders(): Promise<boolean> {
    if (!dirtySortItems.value.length) return false;
    operationError.value = null;
    try {
      await api.updateFriendLinkSortOrders(dirtySortItems.value);
      await refreshAfterOperation();
      return true;
    } catch (reason) {
      operationError.value = { action: "sort", kind: errorKind(reason) };
      return false;
    }
  }

  async function runRowOperation(
    id: string,
    action: FriendLinkOperationError["action"],
    operation: () => Promise<unknown>
  ): Promise<boolean> {
    operationError.value = null;
    operatingId.value = id;
    try {
      await operation();
      await refreshAfterOperation();
      return true;
    } catch (reason) {
      operationError.value = { action, kind: errorKind(reason) };
      return false;
    } finally {
      operatingId.value = null;
    }
  }

  async function updateStatus(
    id: string,
    status: FriendLinkStatus
  ): Promise<boolean> {
    return runRowOperation(id, "status", () =>
      api.updateFriendLinkStatus(id, status)
    );
  }

  async function remove(id: string): Promise<boolean> {
    return runRowOperation(id, "delete", () => api.deleteFriendLink(id));
  }

  return {
    filters,
    items,
    total,
    loading,
    error,
    operationError,
    operatingId,
    dialogOpen,
    editingId,
    form,
    formErrors,
    saving,
    sortDrafts,
    dirtySortItems,
    initialize,
    search,
    reset,
    refresh,
    changePage,
    openCreate,
    openEdit,
    closeDialog,
    save,
    setSortOrder,
    saveSortOrders,
    updateStatus,
    remove
  };
}
