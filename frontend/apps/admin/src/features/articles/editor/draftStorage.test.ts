import { afterEach, describe, expect, it } from "vitest";
import { createEmptyArticleForm } from "./form";
import {
  articleDraftKey,
  clearArticleDraft,
  loadArticleDraft,
  saveArticleDraft
} from "./draftStorage";

afterEach(() => localStorage.clear());

describe("article editor draft storage", () => {
  it("uses separate keys for create and edit drafts", () => {
    expect(articleDraftKey("create")).toBe(
      "myblog-admin:article-editor-draft:create"
    );
    expect(articleDraftKey("edit", "100")).toBe(
      "myblog-admin:article-editor-draft:edit:100"
    );
  });

  it("saves, loads and clears a draft", () => {
    const form = createEmptyArticleForm();
    form.titleZh = "草稿标题";
    form.body = "草稿正文";

    saveArticleDraft("create", undefined, form);

    expect(loadArticleDraft("create")?.titleZh).toBe("草稿标题");
    clearArticleDraft("create");
    expect(loadArticleDraft("create")).toBeNull();
  });

  it("returns null for invalid json draft content", () => {
    localStorage.setItem(articleDraftKey("create"), "{invalid");

    expect(loadArticleDraft("create")).toBeNull();
  });
});
