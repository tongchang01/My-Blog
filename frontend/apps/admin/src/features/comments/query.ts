import type { CommentListFilters, CommentListParams } from "./model";

export function buildCommentListParams(
  filters: CommentListFilters
): CommentListParams {
  const targetId = filters.targetId.trim();
  const keyword = filters.keyword.trim();
  return {
    ...(filters.targetType === "ALL" ? {} : { targetType: filters.targetType }),
    ...(targetId ? { targetId } : {}),
    ...(filters.auditStatus === "ALL"
      ? {}
      : { auditStatus: filters.auditStatus }),
    ...(keyword ? { keyword } : {}),
    includeDeleted: filters.includeDeleted,
    page: filters.page,
    size: filters.size
  };
}
