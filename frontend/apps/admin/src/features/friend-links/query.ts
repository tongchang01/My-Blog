import type {
  FriendLinkListFilters,
  FriendLinkListParams
} from "./model";

export function buildFriendLinkListParams(
  filters: FriendLinkListFilters
): FriendLinkListParams {
  const keyword = filters.keyword.trim();
  return {
    ...(keyword ? { keyword } : {}),
    ...(filters.status === "ALL" ? {} : { status: filters.status }),
    page: filters.page,
    size: filters.size
  };
}
