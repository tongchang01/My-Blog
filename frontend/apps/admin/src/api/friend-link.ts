import type { ApiResponse } from "./contract";
import type {
  FriendLinkItem,
  FriendLinkListFilters,
  FriendLinkPageResponse,
  FriendLinkSortItem,
  FriendLinkStatus,
  FriendLinkWritePayload
} from "@/features/friend-links/model";
import { buildFriendLinkListParams } from "@/features/friend-links/query";
import { http } from "@/utils/http";

export const listFriendLinks = (filters: FriendLinkListFilters) =>
  http.get<ApiResponse<FriendLinkPageResponse>>(
    "/api/admin/friend-links",
    { params: buildFriendLinkListParams(filters) }
  );

export const getFriendLink = (id: string) =>
  http.get<ApiResponse<FriendLinkItem>>(`/api/admin/friend-links/${id}`);

export const createFriendLink = (payload: FriendLinkWritePayload) =>
  http.post<ApiResponse<FriendLinkItem>>("/api/admin/friend-links", {
    data: payload
  });

export const updateFriendLink = (
  id: string,
  payload: FriendLinkWritePayload
) =>
  http.request<ApiResponse<FriendLinkItem>>(
    "put",
    `/api/admin/friend-links/${id}`,
    { data: payload }
  );

export const updateFriendLinkStatus = (
  id: string,
  status: FriendLinkStatus
) =>
  http.request<ApiResponse<FriendLinkItem>>(
    "patch",
    `/api/admin/friend-links/${id}/status`,
    { data: { status } }
  );

export const updateFriendLinkSortOrders = (items: FriendLinkSortItem[]) =>
  http.request<ApiResponse<FriendLinkItem[]>>(
    "put",
    "/api/admin/friend-links/sort-orders",
    { data: { items } }
  );

export const deleteFriendLink = (id: string) =>
  http.request<ApiResponse<null>>(
    "delete",
    `/api/admin/friend-links/${id}`
  );
