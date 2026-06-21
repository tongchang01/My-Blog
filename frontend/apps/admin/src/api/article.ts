import type { ApiResponse } from "./contract";
import type {
  ArticleListFilters,
  ArticleListItem,
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
