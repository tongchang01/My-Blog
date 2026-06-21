import type { ApiResponse } from "./contract";
import type {
  CategoryItem,
  CategorySortItem,
  CategoryWritePayload,
  TagItem,
  TagWritePayload
} from "@/features/taxonomy/model";
import { http } from "@/utils/http";

export const listCategories = () =>
  http.get<ApiResponse<CategoryItem[]>>("/api/admin/categories");

export const getCategory = (id: string) =>
  http.get<ApiResponse<CategoryItem>>(`/api/admin/categories/${id}`);

export const createCategory = (payload: CategoryWritePayload) =>
  http.post<ApiResponse<CategoryItem>>("/api/admin/categories", {
    data: payload
  });

export const updateCategory = (
  id: string,
  payload: CategoryWritePayload
) =>
  http.request<ApiResponse<CategoryItem>>(
    "put",
    `/api/admin/categories/${id}`,
    { data: payload }
  );

export const updateCategorySortOrders = (items: CategorySortItem[]) =>
  http.request<ApiResponse<null>>(
    "put",
    "/api/admin/categories/sort-orders",
    { data: { items } }
  );

export const deleteCategory = (id: string) =>
  http.request<ApiResponse<null>>(
    "delete",
    `/api/admin/categories/${id}`
  );

export const listTags = () =>
  http.get<ApiResponse<TagItem[]>>("/api/admin/tags");

export const getTag = (id: string) =>
  http.get<ApiResponse<TagItem>>(`/api/admin/tags/${id}`);

export const createTag = (payload: TagWritePayload) =>
  http.post<ApiResponse<TagItem>>("/api/admin/tags", { data: payload });

export const updateTag = (id: string, payload: TagWritePayload) =>
  http.request<ApiResponse<TagItem>>("put", `/api/admin/tags/${id}`, {
    data: payload
  });

export const deleteTag = (id: string) =>
  http.request<ApiResponse<null>>("delete", `/api/admin/tags/${id}`);
