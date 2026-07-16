import { afterEach, describe, expect, it } from "vitest";
import { createEmptyArticleForm } from "./form";
import {
  articleDraftKey,
  clearArticleDraft,
  clearArticleDrafts,
  loadArticleDraft,
  saveArticleDraft
} from "./draftStorage";

afterEach(() => localStorage.clear());

describe("article editor draft storage", () => {
  const ownerId = "1001";

  it("uses separate keys for create and edit drafts", () => {
    expect(articleDraftKey(ownerId, "create")).toBe(
      "myblog-admin:article-editor-draft:1001:create"
    );
    expect(articleDraftKey(ownerId, "edit", "100")).toBe(
      "myblog-admin:article-editor-draft:1001:edit:100"
    );
  });

  it("saves, loads and clears a draft", () => {
    const form = createEmptyArticleForm();
    form.titleZh = "草稿标题";
    form.body = "草稿正文";

    saveArticleDraft(ownerId, "create", undefined, form);

    expect(loadArticleDraft(ownerId, "create")?.titleZh).toBe("草稿标题");
    clearArticleDraft(ownerId, "create");
    expect(loadArticleDraft(ownerId, "create")).toBeNull();
  });

  it("does not persist an article password", () => {
    const form = createEmptyArticleForm();
    form.status = "PASSWORD";
    form.password = "plain-text-secret";

    saveArticleDraft(ownerId, "create", undefined, form);

    const stored = JSON.parse(
      localStorage.getItem(articleDraftKey(ownerId, "create")) as string
    );
    expect(stored.form).not.toHaveProperty("password");
    expect(loadArticleDraft(ownerId, "create")?.password).toBe("");
  });

  it("isolates and clears drafts by owner", () => {
    const form = createEmptyArticleForm();
    form.titleZh = "账号 A 草稿";
    saveArticleDraft(ownerId, "create", undefined, form);
    saveArticleDraft("1002", "create", undefined, createEmptyArticleForm());

    expect(loadArticleDraft("1002", "create")?.titleZh).toBe("");
    clearArticleDrafts(ownerId);
    expect(loadArticleDraft(ownerId, "create")).toBeNull();
    expect(loadArticleDraft("1002", "create")).not.toBeNull();
  });

  it("returns null for invalid json draft content", () => {
    localStorage.setItem(articleDraftKey(ownerId, "create"), "{invalid");

    expect(loadArticleDraft(ownerId, "create")).toBeNull();
  });

  it("loads older drafts with current default fields", () => {
    localStorage.setItem(
      articleDraftKey(ownerId, "create"),
      JSON.stringify({
        savedAt: "2026-06-26T10:00:00.000Z",
        form: {
          titleZh: "legacy",
          tagIds: undefined
        }
      })
    );

    expect(loadArticleDraft(ownerId, "create")).toMatchObject({
      titleZh: "legacy",
      homepageSlot: "NONE",
      tagIds: []
    });
  });
});
