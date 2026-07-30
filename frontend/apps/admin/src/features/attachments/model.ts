import type { PageResponse } from "@/features/articles/model";

export interface AttachmentItem {
  id: string;
  publicUrl: string;
  contentType: string;
  fileSize: number;
  width: number;
  height: number;
  originalFilename: string;
  createdAt: string;
  createdBy: string | null;
}

export type AttachmentSortBy = "createdAt" | "fileSize" | "originalFilename";
export type AttachmentSortDirection = "asc" | "desc";

export interface AttachmentPageParams {
  page: number;
  size: number;
}

export interface AttachmentListParams extends AttachmentPageParams {
  sortBy: AttachmentSortBy;
  sortDirection: AttachmentSortDirection;
}

export type AttachmentPageResponse = PageResponse<AttachmentItem>;
