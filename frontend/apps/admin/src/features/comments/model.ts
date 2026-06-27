import type { PageResponse } from "@/features/articles/model";

export type CommentTargetType = "ARTICLE" | "GUESTBOOK";
export type CommentTargetTypeFilter = CommentTargetType | "ALL";
export type CommentAuditStatus = "PASS" | "PENDING" | "HIDDEN";
export type CommentAuditStatusFilter = CommentAuditStatus | "ALL";

export interface CommentListFilters {
  targetType: CommentTargetTypeFilter;
  targetId: string;
  auditStatus: CommentAuditStatusFilter;
  keyword: string;
  includeDeleted: boolean;
  page: number;
  size: number;
}

export interface CommentListParams {
  targetType?: CommentTargetType;
  targetId?: string;
  auditStatus?: CommentAuditStatus;
  keyword?: string;
  includeDeleted: boolean;
  page: number;
  size: number;
}

export interface CommentListItem {
  id: string;
  targetType: CommentTargetType;
  targetId: string;
  parentId: string | null;
  replyToCommentId: string | null;
  replyToNickname: string | null;
  authorNickname: string;
  authorEmail: string | null;
  authorSite: string | null;
  authorIp: string | null;
  authorUserAgent: string | null;
  contentMd: string;
  contentHtml: string;
  auditStatus: CommentAuditStatus;
  createdAt: string;
  deleted: boolean;
}

export type CommentPageResponse = PageResponse<CommentListItem>;

export interface CommentReplyResponse {
  id: string;
  auditStatus: CommentAuditStatus;
}
