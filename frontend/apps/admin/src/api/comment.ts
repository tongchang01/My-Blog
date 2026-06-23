import type { ApiResponse } from "./contract";
import type {
  CommentListFilters,
  CommentPageResponse
} from "@/features/comments/model";
import { buildCommentListParams } from "@/features/comments/query";
import { http } from "@/utils/http";

export const listComments = (filters: CommentListFilters) =>
  http.get<ApiResponse<CommentPageResponse>>("/api/admin/comments", {
    params: buildCommentListParams(filters)
  });

export const approveComment = (id: string) =>
  http.post<ApiResponse<null>>(`/api/admin/comments/${id}/approve`);

export const hideComment = (id: string) =>
  http.post<ApiResponse<null>>(`/api/admin/comments/${id}/hide`);

export const restoreComment = (id: string) =>
  http.post<ApiResponse<null>>(`/api/admin/comments/${id}/restore`);

export const deleteComment = (id: string) =>
  http.request<ApiResponse<null>>("delete", `/api/admin/comments/${id}`);
