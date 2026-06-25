import { describe, expect, it, vi } from "vitest";
import type { ApiResponse } from "@/api/contract";
import type {
  AttachmentItem,
  AttachmentPageResponse
} from "./model";
import {
  type AttachmentManagementApi,
  useAttachmentManagement
} from "./useAttachmentManagement";

function ok<T>(data: T): ApiResponse<T> {
  return { code: "00000", msg: "success", data };
}

function attachment(id: string): AttachmentItem {
  return {
    id,
    publicUrl: `http://localhost/media/${id}.png`,
    contentType: "image/png",
    fileSize: 1024,
    width: 800,
    height: 450,
    originalFilename: `${id}.png`,
    createdAt: "2026-06-25T12:00:00",
    createdBy: "1001"
  };
}

function page(
  records: AttachmentItem[] = [attachment("9007199254743001")],
  currentPage = 1,
  total = records.length
): AttachmentPageResponse {
  return { records, total, page: currentPage, size: 20 };
}

function api(
  overrides: Partial<AttachmentManagementApi> = {}
): AttachmentManagementApi {
  return {
    listAttachments: vi.fn().mockResolvedValue(ok(page())),
    uploadAttachment: vi
      .fn()
      .mockResolvedValue(ok(attachment("9007199254743002"))),
    ...overrides
  };
}

describe("attachment management state", () => {
  it("loads the first attachment page", async () => {
    const source = api();
    const state = useAttachmentManagement(source);

    await state.initialize();

    expect(source.listAttachments).toHaveBeenCalledWith({
      page: 1,
      size: 20
    });
    expect(state.items.value[0].id).toBe("9007199254743001");
    expect(state.loading.value).toBe(false);
    expect(state.error.value).toBeNull();
  });

  it("changes page and refreshes", async () => {
    const source = api();
    const state = useAttachmentManagement(source);

    await state.changePage(2, 50);
    await state.refresh();

    expect(source.listAttachments).toHaveBeenNthCalledWith(1, {
      page: 2,
      size: 50
    });
    expect(source.listAttachments).toHaveBeenNthCalledWith(2, {
      page: 2,
      size: 50
    });
  });

  it("uploads a file then returns to the first page", async () => {
    const source = api();
    const state = useAttachmentManagement(source);
    state.pagination.page = 3;
    const file = new File(["png"], "cover.png", { type: "image/png" });

    await expect(state.upload(file)).resolves.toBe(true);

    expect(source.uploadAttachment).toHaveBeenCalledWith(file);
    expect(state.pagination.page).toBe(1);
    expect(source.listAttachments).toHaveBeenCalledWith({
      page: 1,
      size: 20
    });
    expect(state.uploadError.value).toBeNull();
  });

  it("keeps the current list when upload fails", async () => {
    const source = api({
      uploadAttachment: vi.fn().mockRejectedValue(new Error("invalid image"))
    });
    const state = useAttachmentManagement(source);
    await state.initialize();

    await expect(
      state.upload(new File(["bad"], "bad.txt", { type: "text/plain" }))
    ).resolves.toBe(false);

    expect(state.items.value[0].id).toBe("9007199254743001");
    expect(state.uploadError.value?.message).toBe("invalid image");
  });
});
