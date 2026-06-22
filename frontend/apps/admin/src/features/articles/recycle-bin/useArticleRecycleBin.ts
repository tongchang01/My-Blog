import { ref } from "vue";
import type { ApiResponse } from "@/api/contract";
import {
  listCategories,
  listDeletedArticles,
  restoreArticle
} from "@/api/article";
import {
  ApiClientError,
  type ApiErrorKind
} from "@/utils/http/error";
import type {
  ArticleDetail,
  CategoryItem,
  DeletedArticleListItem,
  PageResponse
} from "../model";

export interface ArticleRecycleBinApi {
  listDeletedArticles(
    page: number,
    size: number
  ): Promise<ApiResponse<PageResponse<DeletedArticleListItem>>>;
  listCategories(): Promise<ApiResponse<CategoryItem[]>>;
  restoreArticle(id: string): Promise<ApiResponse<ArticleDetail>>;
}

export interface RecycleBinOperationError {
  kind: ApiErrorKind;
}

const defaultApi: ArticleRecycleBinApi = {
  listDeletedArticles,
  listCategories,
  restoreArticle
};

function asError(reason: unknown): Error {
  return reason instanceof Error ? reason : new Error(String(reason));
}

function operationError(reason: unknown): RecycleBinOperationError {
  return {
    kind: reason instanceof ApiClientError ? reason.kind : "unknown"
  };
}

export function useArticleRecycleBin(
  api: ArticleRecycleBinApi = defaultApi
) {
  const items = ref<DeletedArticleListItem[]>([]);
  const categories = ref<CategoryItem[]>([]);
  const page = ref(1);
  const size = ref(20);
  const total = ref(0);
  const loading = ref(false);
  const error = ref<Error | null>(null);
  const operationErrorState = ref<RecycleBinOperationError | null>(null);
  const restoringId = ref<string | null>(null);
  let requestVersion = 0;

  async function load(): Promise<void> {
    const version = ++requestVersion;
    loading.value = true;
    error.value = null;
    try {
      const response = await api.listDeletedArticles(page.value, size.value);
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

  async function loadCategories(): Promise<void> {
    try {
      categories.value = (await api.listCategories()).data;
    } catch {
      categories.value = [];
    }
  }

  async function initialize(): Promise<void> {
    await Promise.all([load(), loadCategories()]);
  }

  async function retry(): Promise<void> {
    await load();
  }

  async function refresh(): Promise<void> {
    await load();
  }

  async function changePage(nextPage: number, nextSize = size.value) {
    page.value = nextPage;
    size.value = nextSize;
    await load();
  }

  async function restore(id: string): Promise<boolean> {
    operationErrorState.value = null;
    restoringId.value = id;
    try {
      await api.restoreArticle(id);
      await load();
      if (
        !error.value &&
        items.value.length === 0 &&
        total.value > 0 &&
        page.value > 1
      ) {
        page.value -= 1;
        await load();
      }
      return true;
    } catch (reason) {
      operationErrorState.value = operationError(reason);
      return false;
    } finally {
      restoringId.value = null;
    }
  }

  return {
    items,
    categories,
    page,
    size,
    total,
    loading,
    error,
    operationError: operationErrorState,
    restoringId,
    initialize,
    retry,
    refresh,
    changePage,
    restore
  };
}
