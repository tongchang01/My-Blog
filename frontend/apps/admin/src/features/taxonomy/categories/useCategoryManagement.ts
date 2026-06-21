import { computed, reactive, ref } from "vue";
import type { ApiResponse } from "@/api/contract";
import {
  createCategory,
  deleteCategory,
  listCategories,
  updateCategory,
  updateCategorySortOrders
} from "@/api/taxonomy";
import { ApiClientError, type ApiErrorKind } from "@/utils/http/error";
import {
  categoryFormToPayload,
  categoryToForm,
  createCategoryForm,
  validateCategoryForm,
  type CategoryForm,
  type CategoryFormErrors
} from "../form";
import type {
  CategoryItem,
  CategorySortItem,
  CategoryWritePayload
} from "../model";

export interface CategoryManagementApi {
  listCategories(): Promise<ApiResponse<CategoryItem[]>>;
  createCategory(
    payload: CategoryWritePayload
  ): Promise<ApiResponse<CategoryItem>>;
  updateCategory(
    id: string,
    payload: CategoryWritePayload
  ): Promise<ApiResponse<CategoryItem>>;
  updateCategorySortOrders(
    items: CategorySortItem[]
  ): Promise<ApiResponse<null>>;
  deleteCategory(id: string): Promise<ApiResponse<null>>;
}

const defaultApi: CategoryManagementApi = {
  listCategories,
  createCategory,
  updateCategory,
  updateCategorySortOrders,
  deleteCategory
};

export interface CategoryOperationError {
  action: "save" | "sort" | "delete";
  kind: ApiErrorKind;
}

function errorKind(reason: unknown): ApiErrorKind {
  return reason instanceof ApiClientError ? reason.kind : "unknown";
}

function clearRecord(record: Record<string, unknown>): void {
  Object.keys(record).forEach(key => delete record[key]);
}

export function useCategoryManagement(
  api: CategoryManagementApi = defaultApi
) {
  const items = ref<CategoryItem[]>([]);
  const keyword = ref("");
  const loading = ref(false);
  const requestError = ref<Error | null>(null);
  const operationError = ref<CategoryOperationError | null>(null);
  const dialogOpen = ref(false);
  const editingId = ref<string | null>(null);
  const form = reactive<CategoryForm>(createCategoryForm());
  const formErrors = reactive<CategoryFormErrors>({});
  const saving = ref(false);
  const sortDrafts = reactive<Record<string, number>>({});

  const filteredItems = computed(() => {
    const normalized = keyword.value.trim().toLocaleLowerCase();
    if (!normalized) return items.value;
    return items.value.filter(item =>
      [item.nameZh, item.nameJa, item.nameEn, item.slug].some(value =>
        value?.toLocaleLowerCase().includes(normalized)
      )
    );
  });

  const dirtySortItems = computed<CategorySortItem[]>(() =>
    items.value
      .filter(item => sortDrafts[item.id] !== item.sortOrder)
      .map(item => ({ id: item.id, sortOrder: sortDrafts[item.id] }))
  );

  async function load(): Promise<void> {
    loading.value = true;
    requestError.value = null;
    try {
      items.value = (await api.listCategories()).data;
      clearRecord(sortDrafts);
      items.value.forEach(item => {
        sortDrafts[item.id] = item.sortOrder;
      });
    } catch (reason) {
      requestError.value =
        reason instanceof Error ? reason : new Error(String(reason));
    } finally {
      loading.value = false;
    }
  }

  function resetForm(next: CategoryForm): void {
    Object.assign(form, next);
    clearRecord(formErrors);
    operationError.value = null;
  }

  function openCreate(): void {
    editingId.value = null;
    resetForm(createCategoryForm());
    dialogOpen.value = true;
  }

  function openEdit(item: CategoryItem): void {
    editingId.value = item.id;
    resetForm(categoryToForm(item));
    dialogOpen.value = true;
  }

  function closeDialog(): void {
    dialogOpen.value = false;
  }

  async function save(): Promise<boolean> {
    const errors = validateCategoryForm(form);
    clearRecord(formErrors);
    Object.assign(formErrors, errors);
    if (Object.keys(errors).length) return false;
    saving.value = true;
    operationError.value = null;
    try {
      const payload = categoryFormToPayload(form);
      if (editingId.value) {
        await api.updateCategory(editingId.value, payload);
      } else {
        await api.createCategory(payload);
      }
      dialogOpen.value = false;
      await load();
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
      await api.updateCategorySortOrders(dirtySortItems.value);
      await load();
      return true;
    } catch (reason) {
      operationError.value = { action: "sort", kind: errorKind(reason) };
      return false;
    }
  }

  async function remove(id: string): Promise<boolean> {
    operationError.value = null;
    try {
      await api.deleteCategory(id);
      await load();
      return true;
    } catch (reason) {
      operationError.value = { action: "delete", kind: errorKind(reason) };
      return false;
    }
  }

  return {
    items,
    filteredItems,
    keyword,
    loading,
    requestError,
    operationError,
    dialogOpen,
    editingId,
    form,
    formErrors,
    saving,
    sortDrafts,
    dirtySortItems,
    initialize: load,
    retry: load,
    openCreate,
    openEdit,
    closeDialog,
    save,
    setSortOrder,
    saveSortOrders,
    remove
  };
}
