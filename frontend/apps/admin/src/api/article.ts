import type { ApiResponse } from "./contract";
import type {
  ArticleListFilters,
  ArticleListItem,
  ArticleDetail,
  ArticleWritePayload,
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
