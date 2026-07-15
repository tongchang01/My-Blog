export type FriendLinkStatus = "VISIBLE" | "HIDDEN";

export interface FriendLinkItem {
  id: string;
  name: string;
  url: string;
  avatarUrl: string | null;
  description: string | null;
  sortOrder: number;
  status: FriendLinkStatus;
  createdAt: string;
  createdBy: string | null;
  updatedAt: string;
  updatedBy: string | null;
}

export interface FriendLinkPageResponse {
  records: FriendLinkItem[];
  total: number;
  page: number;
  size: number;
}

export interface FriendLinkListFilters {
  page: number;
  size: number;
}

export interface FriendLinkWritePayload {
  name: string;
  url: string;
  avatarUrl: string | null;
  description: string | null;
  sortOrder: number;
  status: FriendLinkStatus;
}

export interface FriendLinkSortItem {
  id: string;
  sortOrder: number;
}
