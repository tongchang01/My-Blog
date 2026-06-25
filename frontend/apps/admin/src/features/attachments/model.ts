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

export interface AttachmentListParams {
  page: number;
  size: number;
}

export type AttachmentPageResponse = PageResponse<AttachmentItem>;
