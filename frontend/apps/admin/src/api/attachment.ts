import type { ApiResponse } from "./contract";
import type {
  AttachmentItem,
  AttachmentListParams,
  AttachmentPageResponse
} from "@/features/attachments/model";
import { http } from "@/utils/http";

export const listAttachments = (params: AttachmentListParams) =>
  http.get<ApiResponse<AttachmentPageResponse>>(
    "/api/admin/attachments",
    { params }
  );

export const getAttachment = (id: string) =>
  http.get<ApiResponse<AttachmentItem>>(`/api/admin/attachments/${id}`);

export const uploadAttachment = (file: File) => {
  const data = new FormData();
  data.append("file", file);
  return http.post<ApiResponse<AttachmentItem>>("/api/admin/attachments", {
    data,
    headers: {
      "Content-Type": "multipart/form-data"
    }
  });
};
