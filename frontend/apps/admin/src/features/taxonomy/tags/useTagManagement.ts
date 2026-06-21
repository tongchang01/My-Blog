import { computed, reactive, ref } from "vue";
import type { ApiResponse } from "@/api/contract";
import { createTag, deleteTag, listTags, updateTag } from "@/api/taxonomy";
import { ApiClientError, type ApiErrorKind } from "@/utils/http/error";
import {
  createTagForm,
  tagFormToPayload,
  tagToForm,
  validateTagForm,
  type TagForm,
  type TagFormErrors
} from "../form";
import type { TagItem, TagWritePayload } from "../model";

export interface TagManagementApi {
  listTags(): Promise<ApiResponse<TagItem[]>>;
  createTag(payload: TagWritePayload): Promise<ApiResponse<TagItem>>;
  updateTag(
    id: string,
    payload: TagWritePayload
  ): Promise<ApiResponse<TagItem>>;
  deleteTag(id: string): Promise<ApiResponse<null>>;
}

const defaultApi: TagManagementApi = {
  listTags,
  createTag,
  updateTag,
  deleteTag
};

export interface TagOperationError {
  action: "save" | "delete";
  kind: ApiErrorKind;
}

function errorKind(reason: unknown): ApiErrorKind {
  return reason instanceof ApiClientError ? reason.kind : "unknown";
}

function clearRecord(record: Record<string, unknown>): void {
  Object.keys(record).forEach(key => delete record[key]);
}

export function useTagManagement(api: TagManagementApi = defaultApi) {
  const items = ref<TagItem[]>([]);
  const keyword = ref("");
  const loading = ref(false);
  const requestError = ref<Error | null>(null);
  const operationError = ref<TagOperationError | null>(null);
  const dialogOpen = ref(false);
  const editingId = ref<string | null>(null);
  const form = reactive<TagForm>(createTagForm());
  const formErrors = reactive<TagFormErrors>({});
  const saving = ref(false);

  const filteredItems = computed(() => {
    const normalized = keyword.value.trim().toLocaleLowerCase();
    if (!normalized) return items.value;
    return items.value.filter(item =>
      [item.nameZh, item.nameJa, item.nameEn, item.slug].some(value =>
        value?.toLocaleLowerCase().includes(normalized)
      )
    );
  });

  async function load(): Promise<void> {
    loading.value = true;
    requestError.value = null;
    try {
      items.value = (await api.listTags()).data;
    } catch (reason) {
      requestError.value =
        reason instanceof Error ? reason : new Error(String(reason));
    } finally {
      loading.value = false;
    }
  }

  function resetForm(next: TagForm): void {
    Object.assign(form, next);
    clearRecord(formErrors);
    operationError.value = null;
  }

  function openCreate(): void {
    editingId.value = null;
    resetForm(createTagForm());
    dialogOpen.value = true;
  }

  function openEdit(item: TagItem): void {
    editingId.value = item.id;
    resetForm(tagToForm(item));
    dialogOpen.value = true;
  }

  function closeDialog(): void {
    dialogOpen.value = false;
  }

  async function save(): Promise<boolean> {
    const errors = validateTagForm(form);
    clearRecord(formErrors);
    Object.assign(formErrors, errors);
    if (Object.keys(errors).length) return false;
    saving.value = true;
    operationError.value = null;
    try {
      const payload = tagFormToPayload(form);
      if (editingId.value) {
        await api.updateTag(editingId.value, payload);
      } else {
        await api.createTag(payload);
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

  async function remove(id: string): Promise<boolean> {
    operationError.value = null;
    try {
      await api.deleteTag(id);
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
    initialize: load,
    retry: load,
    openCreate,
    openEdit,
    closeDialog,
    save,
    remove
  };
}
