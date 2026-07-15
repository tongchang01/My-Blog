import { describe, expect, it, vi } from "vitest";
import type { ApiResponse } from "@/api/contract";
import type {
  CommentListItem,
  CommentPageResponse,
  CommentReplyResponse
} from "./model";
import {
  MAX_COMMENT_REPLY_LENGTH,
  useCommentManagement,
  type CommentManagementApi
} from "./useCommentManagement";

function ok<T>(data: T): ApiResponse<T> {
  return { code: "00000", msg: "success", data };
}

function comment(id: string): CommentListItem {
  return {
    id,
    targetType: "ARTICLE",
    targetId: "9007199254740993",
    parentId: null,
    replyToCommentId: null,
    replyToNickname: null,
    authorNickname: "TYB",
    authorEmail: "tyb@example.com",
    authorSite: "https://example.com",
    authorIp: "127.0.0.1",
    authorUserAgent: "Vitest",
    contentMd: `hello ${id}`,
    contentHtml: `<p>hello ${id}</p>`,
    auditStatus: "PENDING",
    createdAt: "2026-06-23T12:00:00",
    deleted: false
  };
}

function page(id: string, currentPage = 1): CommentPageResponse {
  return {
    records: [comment(id)],
    total: 1,
    page: currentPage,
    size: 20
  };
}

function api(
  overrides: Partial<CommentManagementApi> = {}
): CommentManagementApi {
  return {
    listComments: vi.fn().mockResolvedValue(ok(page("1"))),
    approveComment: vi.fn().mockResolvedValue(ok(null)),
    hideComment: vi.fn().mockResolvedValue(ok(null)),
    deleteComment: vi.fn().mockResolvedValue(ok(null)),
    restoreComment: vi.fn().mockResolvedValue(ok(null)),
    replyComment: vi.fn().mockResolvedValue(
      ok<CommentReplyResponse>({
        id: "reply-1",
        auditStatus: "PASS"
      })
    ),
    ...overrides
  };
}

describe("comment management state", () => {
  it("loads the first page with default filters", async () => {
    const source = api();
    const state = useCommentManagement(source);

    await state.initialize();

    expect(source.listComments).toHaveBeenCalledWith({
      targetType: "ALL",
      targetId: "",
      auditStatus: "ALL",
      keyword: "",
      includeDeleted: false,
      page: 1,
      size: 20
    });
    expect(state.items.value[0].id).toBe("1");
    expect(state.total.value).toBe(1);
    expect(state.loading.value).toBe(false);
  });

  it("searches from page one, refreshes current filters and resets defaults", async () => {
    const source = api();
    const state = useCommentManagement(source);
    state.filters.targetType = "ARTICLE";
    state.filters.targetId = "100";
    state.filters.auditStatus = "PENDING";
    state.filters.keyword = "hello";
    state.filters.includeDeleted = true;
    state.filters.page = 3;

    await state.search();
    expect(source.listComments).toHaveBeenLastCalledWith(
      expect.objectContaining({ page: 1, targetId: "100" })
    );

    state.filters.page = 2;
    await state.refresh();
    expect(source.listComments).toHaveBeenLastCalledWith(
      expect.objectContaining({ page: 2, targetId: "100" })
    );

    await state.reset();
    expect(state.filters).toMatchObject({
      targetType: "ALL",
      targetId: "",
      auditStatus: "ALL",
      keyword: "",
      includeDeleted: false,
      page: 1,
      size: 20
    });
  });

  it("runs moderation commands and refreshes the current page", async () => {
    const source = api();
    const state = useCommentManagement(source);
    await state.initialize();

    await expect(state.approve("1")).resolves.toBe(true);
    await expect(state.hide("1")).resolves.toBe(true);
    await expect(state.remove("1")).resolves.toBe(true);
    await expect(state.restore("1")).resolves.toBe(true);

    expect(source.approveComment).toHaveBeenCalledWith("1");
    expect(source.hideComment).toHaveBeenCalledWith("1");
    expect(source.deleteComment).toHaveBeenCalledWith("1");
    expect(source.restoreComment).toHaveBeenCalledWith("1");
    expect(source.listComments).toHaveBeenCalledTimes(5);
    expect(state.operatingId.value).toBeNull();
    expect(state.operationError.value).toBeNull();
  });

  it("returns to the previous page when an operation empties the last page", async () => {
    const listComments = vi
      .fn()
      .mockResolvedValueOnce(ok({ ...page("21", 2), total: 21 }))
      .mockResolvedValueOnce(
        ok({ records: [], total: 20, page: 2, size: 20 })
      )
      .mockResolvedValueOnce(ok({ ...page("20"), total: 20 }));
    const source = api({ listComments });
    const state = useCommentManagement(source);
    state.filters.page = 2;
    await state.refresh();

    await expect(state.remove("21")).resolves.toBe(true);

    expect(state.filters.page).toBe(1);
    expect(state.items.value[0].id).toBe("20");
    expect(listComments).toHaveBeenCalledTimes(3);
  });

  it("keeps current data and exposes operation errors", async () => {
    const source = api({
      hideComment: vi.fn().mockRejectedValue(new Error("hide failed"))
    });
    const state = useCommentManagement(source);
    await state.initialize();

    await expect(state.hide("1")).resolves.toBe(false);

    expect(state.items.value[0].id).toBe("1");
    expect(state.operationError.value?.message).toBe("hide failed");
    expect(source.listComments).toHaveBeenCalledOnce();
  });

  it("opens and closes the reply dialog with a clean draft", async () => {
    const source = api();
    const state = useCommentManagement(source);
    const target = comment("1");
    state.replyContent.value = "old draft";

    state.openReplyDialog(target);

    expect(state.replyDialogVisible.value).toBe(true);
    expect(state.replyTarget.value).toEqual(target);
    expect(state.replyContent.value).toBe("");

    state.closeReplyDialog();

    expect(state.replyDialogVisible.value).toBe(false);
    expect(state.replyTarget.value).toBeNull();
    expect(state.replyContent.value).toBe("");
  });

  it("submits a trimmed reply and refreshes comments", async () => {
    const source = api();
    const state = useCommentManagement(source);
    await state.initialize();
    state.openReplyDialog(comment("1"));
    state.replyContent.value = "  谢谢反馈  ";

    await expect(state.submitReply()).resolves.toBe(true);

    expect(source.replyComment).toHaveBeenCalledWith("1", "谢谢反馈");
    expect(source.listComments).toHaveBeenCalledTimes(2);
    expect(state.replyDialogVisible.value).toBe(false);
    expect(state.replyTarget.value).toBeNull();
    expect(state.replyContent.value).toBe("");
    expect(state.replySubmitting.value).toBe(false);
  });

  it("does not submit an empty reply", async () => {
    const source = api();
    const state = useCommentManagement(source);
    state.openReplyDialog(comment("1"));
    state.replyContent.value = "   ";

    await expect(state.submitReply()).resolves.toBe(false);

    expect(source.replyComment).not.toHaveBeenCalled();
    expect(state.replyDialogVisible.value).toBe(true);
  });

  it("does not submit a reply longer than the server limit", async () => {
    const source = api();
    const state = useCommentManagement(source);
    state.openReplyDialog(comment("1"));
    state.replyContent.value = "a".repeat(MAX_COMMENT_REPLY_LENGTH + 1);

    await expect(state.submitReply()).resolves.toBe(false);

    expect(source.replyComment).not.toHaveBeenCalled();
    expect(state.replyDialogVisible.value).toBe(true);
  });

  it("keeps the reply dialog open when submit fails", async () => {
    const source = api({
      replyComment: vi.fn().mockRejectedValue(new Error("reply failed"))
    });
    const state = useCommentManagement(source);
    state.openReplyDialog(comment("1"));
    state.replyContent.value = "谢谢反馈";

    await expect(state.submitReply()).resolves.toBe(false);

    expect(state.replyDialogVisible.value).toBe(true);
    expect(state.replyTarget.value?.id).toBe("1");
    expect(state.operationError.value?.message).toBe("reply failed");
    expect(state.replySubmitting.value).toBe(false);
  });

  it("exposes list request failure for retry", async () => {
    const source = api({
      listComments: vi.fn().mockRejectedValue(new Error("offline"))
    });
    const state = useCommentManagement(source);

    await state.refresh();

    expect(state.error.value?.message).toBe("offline");
    expect(state.loading.value).toBe(false);
  });
});
