import type { ApiResponse } from "./contract";
import type {
  ArticleListFilters,
  ArticleListItem,
  ArticleDetail,
  ArticleWritePayload,
  DeletedArticleListItem,
  PageResponse
} from "@/features/articles/model";
import { buildArticleListParams } from "@/features/articles/query";
import { http } from "@/utils/http";

export const listArticles = (filters: ArticleListFilters) =>
  http.get<ApiResponse<PageResponse<ArticleListItem>>>(
    "/api/admin/articles",
    { params: buildArticleListParams(filters) }
  );

export { listCategories, listTags } from "./taxonomy";

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

export const deleteArticle = (id: string) =>
  http.request<ApiResponse<null>>("delete", `/api/admin/articles/${id}`);

export const listDeletedArticles = (page: number, size: number) =>
  http.get<ApiResponse<PageResponse<DeletedArticleListItem>>>(
    "/api/admin/articles/recycle-bin",
    { params: { page, size } }
  );

export const restoreArticle = (id: string) =>
  http.post<ApiResponse<ArticleDetail>>(
    `/api/admin/articles/${id}/restore`
  );
