import { reactive, ref } from "vue";
import type { ApiResponse } from "@/api/contract";
import {
  createArticle,
  getArticle,
  listCategories,
  listTags,
  updateArticle
} from "@/api/article";
import type {
  ArticleDetail,
  ArticleWritePayload,
  CategoryItem,
  TagItem
} from "../model";
import {
  articleDetailToForm,
  articleFormToPayload,
  createEmptyArticleForm,
  validateArticleForm,
  type ArticleEditorMode,
  type ArticleFormErrors
} from "./form";

export interface ArticleEditorApi {
  getArticle(id: string): Promise<ApiResponse<ArticleDetail>>;
  createArticle(
    payload: ArticleWritePayload
  ): Promise<ApiResponse<ArticleDetail>>;
  updateArticle(
    id: string,
    payload: ArticleWritePayload
  ): Promise<ApiResponse<ArticleDetail>>;
  listCategories(): Promise<ApiResponse<CategoryItem[]>>;
  listTags(): Promise<ApiResponse<TagItem[]>>;
}

const defaultApi: ArticleEditorApi = {
  getArticle,
  createArticle,
  updateArticle,
  listCategories,
  listTags
};

function asError(reason: unknown): Error {
  return reason instanceof Error ? reason : new Error(String(reason));
}

export function useArticleEditor(
  mode: ArticleEditorMode,
  articleId?: string,
  api: ArticleEditorApi = defaultApi
) {
  const form = reactive(createEmptyArticleForm());
  const categories = ref<CategoryItem[]>([]);
  const tags = ref<TagItem[]>([]);
  const errors = ref<ArticleFormErrors>({});
  const requestError = ref<Error | null>(null);
  const loading = ref(false);
  const saving = ref(false);

  async function initialize(): Promise<void> {
    loading.value = true;
    requestError.value = null;
    try {
      const [categoryResponse, tagResponse, detailResponse] = await Promise.all([
        api.listCategories(),
        api.listTags(),
        mode === "edit" && articleId ? api.getArticle(articleId) : null
      ]);
      categories.value = categoryResponse.data;
      tags.value = tagResponse.data;
      if (detailResponse) Object.assign(form, articleDetailToForm(detailResponse.data));
    } catch (reason) {
      requestError.value = asError(reason);
      throw reason;
    } finally {
      loading.value = false;
    }
  }

  async function save(): Promise<ArticleDetail | null> {
    errors.value = validateArticleForm(form, mode);
    if (Object.keys(errors.value).length > 0) return null;
    saving.value = true;
    requestError.value = null;
    try {
      const payload = articleFormToPayload(form);
      const response =
        mode === "edit" && articleId
          ? await api.updateArticle(articleId, payload)
          : await api.createArticle(payload);
      return response.data;
    } catch (reason) {
      requestError.value = asError(reason);
      throw reason;
    } finally {
      saving.value = false;
    }
  }

  return {
    form,
    categories,
    tags,
    errors,
    requestError,
    loading,
    saving,
    initialize,
    save
  };
}
