import type { ArticleEditorMode, ArticleForm } from "./form";
import { createEmptyArticleForm } from "./form";

const DRAFT_PREFIX = "myblog-admin:article-editor-draft";

export function articleDraftKey(
  ownerId: string,
  mode: ArticleEditorMode,
  articleId?: string
): string {
  return mode === "edit" && articleId
    ? `${DRAFT_PREFIX}:${ownerId}:edit:${articleId}`
    : `${DRAFT_PREFIX}:${ownerId}:create`;
}

export function saveArticleDraft(
  ownerId: string,
  mode: ArticleEditorMode,
  articleId: string | undefined,
  form: ArticleForm
): void {
  const draftForm: Partial<ArticleForm> = {
    ...form,
    tagIds: [...form.tagIds]
  };
  delete draftForm.password;
  localStorage.setItem(
    articleDraftKey(ownerId, mode, articleId),
    JSON.stringify({
      savedAt: new Date().toISOString(),
      form: draftForm
    })
  );
}

export function loadArticleDraft(
  ownerId: string,
  mode: ArticleEditorMode,
  articleId?: string
): ArticleForm | null {
  const raw = localStorage.getItem(articleDraftKey(ownerId, mode, articleId));
  if (!raw) return null;
  try {
    const parsed = JSON.parse(raw);
    return parsed?.form
      ? {
          ...createEmptyArticleForm(),
          ...parsed.form,
          tagIds: [...(parsed.form.tagIds ?? [])],
          password: ""
        }
      : null;
  } catch {
    return null;
  }
}

export function clearArticleDraft(
  ownerId: string,
  mode: ArticleEditorMode,
  articleId?: string
): void {
  localStorage.removeItem(articleDraftKey(ownerId, mode, articleId));
}

export function clearArticleDrafts(ownerId?: string): void {
  const prefix = ownerId
    ? `${DRAFT_PREFIX}:${ownerId}:`
    : `${DRAFT_PREFIX}:`;
  Object.keys(localStorage)
    .filter(key => key.startsWith(prefix))
    .forEach(key => localStorage.removeItem(key));
}
