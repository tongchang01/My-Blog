import type { ApiResponse } from "./contract";
import type {
  ArticleListFilters,
  ArticleListItem,
  ArticleDetail,
  ArticleWritePayload,
  CategoryItem,
  PageResponse,
  TagItem
} from "@/features/articles/model";
import { buildArticleListParams } from "@/features/articles/query";
import { http } from "@/utils/http";

export const listArticles = (filters: ArticleListFilters) =>
  http.get<ApiResponse<PageResponse<ArticleListItem>>>(
    "/api/admin/articles",
    { params: buildArticleListParams(filters) }
  );

export const listCategories = () =>
  http.get<ApiResponse<CategoryItem[]>>("/api/admin/categories");

export const listTags = () =>
  http.get<ApiResponse<TagItem[]>>("/api/admin/tags");

export const getArticle = (id: string) =>
  http.get<ApiResponse<ArticleDetail>>(`/api/admin/articles/${id}`);

export const createArticle = (payload: ArticleWritePayload) =>
  http.post<ApiResponse<ArticleDetail>>("/api/admin/articles", {
    data: payload
  });

export const updateArticle = (id: string, payload: ArticleWritePayload) =>
  http.request<ApiResponse<ArticleDetail>>(
    "put",
    `/api/admin/articles/${id}`,
    { data: payload }
  );
