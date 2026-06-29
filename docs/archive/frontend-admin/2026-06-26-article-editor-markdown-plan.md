# Article Editor Markdown Experience Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Improve the article editor with Markdown preview, local draft autosave, and leave-page protection without changing backend APIs.

**Architecture:** Keep article persistence unchanged. Add small frontend-only editor helpers under `src/features/articles/editor/` for Markdown rendering and draft persistence, then wire them into the existing `index.vue` page. Use localStorage for drafts and only store current form snapshots on the user machine.

**Tech Stack:** Vue 3 `<script setup>`, TypeScript, existing Element Plus UI, Vitest, Vue Test Utils, localStorage.

---

## File Structure

- Create: `frontend/apps/admin/src/features/articles/editor/markdownPreview.ts`
  - Safe lightweight Markdown-to-HTML renderer. It escapes raw HTML first, then supports headings, unordered lists, code fences, paragraphs, bold, italic, and inline code.
- Create: `frontend/apps/admin/src/features/articles/editor/markdownPreview.test.ts`
  - Unit tests for escaping and supported syntax.
- Create: `frontend/apps/admin/src/features/articles/editor/draftStorage.ts`
  - Draft key, serialize, save, load, clear helpers.
- Create: `frontend/apps/admin/src/features/articles/editor/draftStorage.test.ts`
  - Unit tests for create/edit draft isolation and invalid draft handling.
- Modify: `frontend/apps/admin/src/features/articles/editor/index.vue`
  - Add preview panel, draft status, restore/clear actions, `beforeunload` and router-leave guard.
- Modify: `frontend/apps/admin/src/features/articles/editor/index.test.ts`
  - Add component tests for preview rendering, autosave restore, and leave confirmation.
- Modify: `frontend/apps/admin/locales/zh-CN.yaml`
- Modify: `frontend/apps/admin/locales/ja.yaml`
- Modify: `frontend/apps/admin/locales/en.yaml`
  - Add editor preview and draft text.
- Modify: `frontend/apps/admin/docs/README.md`
  - Document editor UX state.

## Task 1: Markdown Preview

- [ ] Step 1: Write failing unit tests in `markdownPreview.test.ts`.
  - `# 标题\n\n正文` renders an `<h1>` and `<p>`.
  - `<script>alert(1)</script>` is escaped and does not appear as raw script HTML.
  - fenced code renders escaped code inside `<pre><code>`.
- [ ] Step 2: Run `npm test -- markdownPreview` and confirm failures because the module does not exist.
- [ ] Step 3: Implement `renderMarkdownPreview(markdown: string): string` in `markdownPreview.ts`.
- [ ] Step 4: Run `npm test -- markdownPreview` and confirm pass.
- [ ] Step 5: Add a failing page test in `index.test.ts` that sets `form.body = "# 标题"` and expects `data-testid="article-markdown-preview"` to contain `标题`.
- [ ] Step 6: Wire preview into `index.vue` using `computed(() => renderMarkdownPreview(form.body))` and `v-html` only with the escaped renderer output.
- [ ] Step 7: Run `npm test -- markdownPreview article/editor/index`.
- [ ] Step 8: Commit `接入文章Markdown预览`.

## Task 2: Draft Autosave and Restore

- [ ] Step 1: Write failing tests in `draftStorage.test.ts`.
  - Create mode key is `myblog-admin:article-editor-draft:create`.
  - Edit mode key includes article id, e.g. `myblog-admin:article-editor-draft:edit:100`.
  - Invalid JSON returns `null`.
- [ ] Step 2: Implement `draftStorage.ts`.
- [ ] Step 3: Run `npm test -- draftStorage`.
- [ ] Step 4: Add failing page tests for autosave and restore:
  - Editing title/body saves a draft in localStorage.
  - Existing draft shows restore action.
  - Clicking restore copies draft values into the form.
  - Successful save clears the draft.
- [ ] Step 5: Wire draft behavior into `index.vue`.
- [ ] Step 6: Run `npm test -- draftStorage article/editor/index`.
- [ ] Step 7: Commit `接入文章编辑草稿自动保存`.

## Task 3: Leave Confirmation and Docs

- [ ] Step 1: Add failing page tests for dirty leave protection:
  - `beforeunload` sets `event.returnValue` when form differs from last saved/loaded snapshot.
  - It does not set `returnValue` immediately after initialization.
- [ ] Step 2: Wire `beforeunload` listener and `onBeforeRouteLeave`.
  - Use `window.confirm(transformI18n("articles.editor.leaveConfirm"))` for route leave.
  - Remove listener on unmount.
- [ ] Step 3: Update locale files and README.
- [ ] Step 4: Run complete verification:
  - `npm test`
  - `npm run typecheck`
  - `npm run build`
- [ ] Step 5: Commit `完善文章编辑离开保护与文档`.

## Self-Review

- No backend API changes.
- No new package dependency.
- Markdown preview is explicitly escaped before HTML insertion.
- Drafts are local-only and separated between create and edit pages.
- Leave confirmation is based on form dirty state, not on request status.
