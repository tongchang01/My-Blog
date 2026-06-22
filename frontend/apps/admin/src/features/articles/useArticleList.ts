import { reactive, ref } from "vue";
import type { ApiResponse } from "@/api/contract";
import {
  deleteArticle,
  listArticles,
  listCategories,
  listTags
} from "@/api/article";
import type {
  ArticleListFilters,
  ArticleListItem,
  CategoryItem,
  PageResponse,
  TagItem
} from "./model";

export interface ArticleListApi {
  listArticles(
    filters: ArticleListFilters
  ): Promise<ApiResponse<PageResponse<ArticleListItem>>>;
  listCategories(): Promise<ApiResponse<CategoryItem[]>>;
  listTags(): Promise<ApiResponse<TagItem[]>>;
  deleteArticle(id: string): Promise<ApiResponse<null>>;
}

const defaultApi: ArticleListApi = {
  listArticles,
  listCategories,
  listTags,
  deleteArticle
};

const DEFAULT_FILTERS: ArticleListFilters = {
  titleKeyword: "",
  status: "ALL",
  page: 1,
  size: 20
};

function asError(error: unknown): Error {
  return error instanceof Error ? error : new Error(String(error));
}

export function useArticleList(api: ArticleListApi = defaultApi) {
  const filters = reactive<ArticleListFilters>({ ...DEFAULT_FILTERS });
  const items = ref<ArticleListItem[]>([]);
  const categories = ref<CategoryItem[]>([]);
  const tags = ref<TagItem[]>([]);
  const total = ref(0);
  const loading = ref(false);
  const error = ref<Error | null>(null);
  const operationError = ref<Error | null>(null);
  const deletingId = ref<string | null>(null);
  let requestVersion = 0;

  async function loadArticles(): Promise<void> {
    const version = ++requestVersion;
    loading.value = true;
    error.value = null;
    const requestFilters = { ...filters };
    try {
      const response = await api.listArticles(requestFilters);
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

  async function loadTags(): Promise<void> {
    try {
      tags.value = (await api.listTags()).data;
    } catch {
      tags.value = [];
    }
  }

  async function initialize(): Promise<void> {
    await Promise.all([loadArticles(), loadCategories(), loadTags()]);
  }

  async function search(): Promise<void> {
    filters.page = 1;
    await loadArticles();
  }

  async function reset(): Promise<void> {
    Object.assign(filters, DEFAULT_FILTERS);
    await loadArticles();
  }

  async function refresh(): Promise<void> {
    await loadArticles();
  }

  async function changePage(page: number, size = filters.size): Promise<void> {
    filters.page = page;
    filters.size = size;
    await loadArticles();
  }

  async function remove(id: string): Promise<boolean> {
    operationError.value = null;
    deletingId.value = id;
    try {
      await api.deleteArticle(id);
      await loadArticles();
      if (
        !error.value &&
        items.value.length === 0 &&
        total.value > 0 &&
        filters.page > 1
      ) {
        filters.page -= 1;
        await loadArticles();
      }
      return true;
    } catch (reason) {
      operationError.value = asError(reason);
      return false;
    } finally {
      deletingId.value = null;
    }
  }

  return {
    filters,
    items,
    categories,
    tags,
    total,
    loading,
    error,
    operationError,
    deletingId,
    initialize,
    search,
    reset,
    refresh,
    changePage,
    remove
  };
}
