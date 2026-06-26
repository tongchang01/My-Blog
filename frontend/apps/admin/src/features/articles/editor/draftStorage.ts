import type { ArticleEditorMode, ArticleForm } from "./form";

const DRAFT_PREFIX = "myblog-admin:article-editor-draft";

export function articleDraftKey(
  mode: ArticleEditorMode,
  articleId?: string
): string {
  return mode === "edit" && articleId
    ? `${DRAFT_PREFIX}:edit:${articleId}`
    : `${DRAFT_PREFIX}:create`;
}

export function saveArticleDraft(
  mode: ArticleEditorMode,
  articleId: string | undefined,
  form: ArticleForm
): void {
  localStorage.setItem(
    articleDraftKey(mode, articleId),
    JSON.stringify({
      savedAt: new Date().toISOString(),
      form: { ...form, tagIds: [...form.tagIds] }
    })
  );
}

export function loadArticleDraft(
  mode: ArticleEditorMode,
  articleId?: string
): ArticleForm | null {
  const raw = localStorage.getItem(articleDraftKey(mode, articleId));
  if (!raw) return null;
  try {
    const parsed = JSON.parse(raw);
    return parsed?.form ?? null;
  } catch {
    return null;
  }
}

export function clearArticleDraft(
  mode: ArticleEditorMode,
  articleId?: string
): void {
  localStorage.removeItem(articleDraftKey(mode, articleId));
}
