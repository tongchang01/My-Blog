<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { onBeforeRouteLeave, useRoute, useRouter } from "vue-router";
import { i18n, transformI18n } from "@/plugins/i18n";
import AttachmentPickerDialog from "@/features/attachments/AttachmentPickerDialog.vue";
import type { AttachmentItem } from "@/features/attachments/model";
import type {
  AdminLocale,
  ArticleHomepageSlot,
  ArticleStatus,
  LocalizedNames
} from "../model";
import {
  homepageSlotTranslationKey,
  localizedName,
  statusTranslationKey
} from "../presentation";
import {
  clearArticleDraft,
  loadArticleDraft,
  saveArticleDraft
} from "./draftStorage";
import type { ArticleForm } from "./form";
import { renderMarkdownPreview } from "./markdownPreview";
import { enhanceMarkdownPreview } from "./markdownPreviewEnhance";
import { useArticleEditor } from "./useArticleEditor";

defineOptions({ name: "ArticleEditor" });

const route = useRoute();
const router = useRouter();
const mode = route.name === "ArticleEdit" ? "edit" : "create";
const articleId = typeof route.params.id === "string" ? route.params.id : undefined;
const state = useArticleEditor(mode, articleId);
const {
  form,
  categories,
  tags,
  errors,
  requestError,
  loading,
  saving,
  initialize,
  save
} = state;

const locale = computed(
  () => (i18n.global.locale as unknown as { value: AdminLocale }).value
);
const coverPickerOpen = ref(false);
const draft = ref<ArticleForm | null>(null);
const autosaveReady = ref(false);
const baselineSnapshot = ref<string | null>(null);
const pageTitle = computed(() =>
  transformI18n(
    mode === "edit" ? "articles.editor.editTitle" : "articles.editor.createTitle"
  )
);
const homepageSlotOptions: ArticleHomepageSlot[] = [
  "NONE",
  "PINNED",
  "FEATURED"
];
const previewHtml = computed(() => renderMarkdownPreview(form.body));
const previewElement = ref<HTMLElement | null>(null);
const hasDraft = computed(() => Boolean(draft.value));
const isDirty = computed(
  () => baselineSnapshot.value !== null && snapshotForm() !== baselineSnapshot.value
);

function snapshotForm(): string {
  return JSON.stringify({ ...form, tagIds: [...form.tagIds] });
}

function dictionaryName(item: LocalizedNames): string {
  return localizedName(item, locale.value);
}

function fieldError(field: keyof typeof form): string {
  const code = errors.value[field];
  return code ? transformI18n(`articles.editor.validation.${code}`) : "";
}

async function enhancePreview(): Promise<void> {
  await nextTick();
  if (previewElement.value) await enhanceMarkdownPreview(previewElement.value);
}

function homepageSlotDisabled(slot: ArticleHomepageSlot): boolean {
  return slot !== "NONE" && form.status !== "PUBLISHED";
}

function selectCover(item: AttachmentItem): void {
  form.coverAttachmentId = item.id;
  form.coverUrl = item.publicUrl;
}

function clearCover(): void {
  form.coverAttachmentId = null;
  form.coverUrl = null;
}

function restoreDraft(): void {
  if (!draft.value) return;
  Object.assign(form, {
    ...draft.value,
    tagIds: [...draft.value.tagIds]
  });
}

function clearDraft(): void {
  clearArticleDraft(mode, articleId);
  draft.value = null;
}

async function submit(): Promise<void> {
  try {
    const result = await save();
    if (result) {
      clearDraft();
      baselineSnapshot.value = snapshotForm();
      await router.push("/articles/list");
    }
  } catch {
    // 请求错误由页面中的 alert 展示，表单数据保持不变。
  }
}

watch(
  form,
  () => {
    if (!autosaveReady.value) return;
    saveArticleDraft(mode, articleId, form);
  },
  { deep: true }
);

watch(
  () => form.status,
  status => {
    if (status !== "PUBLISHED") form.homepageSlot = "NONE";
    if (status !== "PASSWORD") form.password = "";
  }
);

watch(previewHtml, () => {
  void enhancePreview();
});

onMounted(async () => {
  try {
    await initialize();
    baselineSnapshot.value = snapshotForm();
    draft.value = loadArticleDraft(mode, articleId);
  } catch {
    // 请求错误由页面中的 alert 展示，表单数据保持不变。
  } finally {
    autosaveReady.value = true;
    void enhancePreview();
  }
});

function handleBeforeUnload(event: BeforeUnloadEvent): void {
  if (!isDirty.value) return;
  event.preventDefault();
  event.returnValue = "";
}

window.addEventListener("beforeunload", handleBeforeUnload);

onBeforeUnmount(() => {
  window.removeEventListener("beforeunload", handleBeforeUnload);
});

onBeforeRouteLeave((_to, _from, next) => {
  if (!isDirty.value || window.confirm(transformI18n("articles.editor.leaveConfirm"))) {
    next();
    return;
  }
  next(false);
});
</script>

<template>
  <section data-testid="article-editor" class="article-editor-page">
    <div class="editor-toolbar">
      <div>
        <h1>{{ pageTitle }}</h1>
        <p>{{ transformI18n("articles.editor.subtitle") }}</p>
      </div>
      <div class="toolbar-actions">
        <el-button @click="router.push('/articles/list')">
          {{ transformI18n("articles.editor.back") }}
        </el-button>
        <el-button
          data-testid="article-save"
          type="primary"
          :loading="saving"
          @click="submit"
        >
          {{ transformI18n("articles.editor.save") }}
        </el-button>
      </div>
    </div>

    <el-alert
      v-if="requestError"
      data-testid="article-editor-error"
      type="error"
      :closable="false"
      :title="transformI18n('articles.editor.requestError')"
      show-icon
    />

    <el-alert
      v-if="hasDraft"
      data-testid="article-draft-alert"
      type="info"
      :closable="false"
      :title="transformI18n('articles.editor.draftFound')"
      show-icon
    >
      <el-button data-testid="article-draft-restore" type="primary" link @click="restoreDraft">
        {{ transformI18n("articles.editor.draftRestore") }}
      </el-button>
      <el-button data-testid="article-draft-clear" link @click="clearDraft">
        {{ transformI18n("articles.editor.draftClear") }}
      </el-button>
    </el-alert>

    <el-skeleton v-if="loading" :rows="10" animated />
    <el-form v-else :model="form" label-position="top" class="editor-grid">
      <el-card shadow="never" class="editor-card content-card">
        <template #header>
          <h2>{{ transformI18n("articles.editor.contentSection") }}</h2>
        </template>
        <div class="language-grid">
          <el-form-item
            :label="transformI18n('articles.editor.titleZh')"
            :error="fieldError('titleZh')"
          >
            <el-input v-model="form.titleZh" maxlength="255" show-word-limit />
          </el-form-item>
          <el-form-item :label="transformI18n('articles.editor.titleJa')" :error="fieldError('titleJa')">
            <el-input v-model="form.titleJa" maxlength="255" show-word-limit />
          </el-form-item>
          <el-form-item :label="transformI18n('articles.editor.titleEn')" :error="fieldError('titleEn')">
            <el-input v-model="form.titleEn" maxlength="255" show-word-limit />
          </el-form-item>
          <el-form-item
            :label="transformI18n('articles.editor.summaryZh')"
            :error="fieldError('summaryZh')"
          >
            <el-input v-model="form.summaryZh" type="textarea" :rows="3" maxlength="500" show-word-limit />
          </el-form-item>
          <el-form-item :label="transformI18n('articles.editor.summaryJa')" :error="fieldError('summaryJa')">
            <el-input v-model="form.summaryJa" type="textarea" :rows="3" maxlength="500" show-word-limit />
          </el-form-item>
          <el-form-item :label="transformI18n('articles.editor.summaryEn')" :error="fieldError('summaryEn')">
            <el-input v-model="form.summaryEn" type="textarea" :rows="3" maxlength="500" show-word-limit />
          </el-form-item>
        </div>
        <el-form-item
          :label="transformI18n('articles.editor.body')"
          :error="fieldError('body')"
        >
          <div class="markdown-workspace">
            <el-input
              v-model="form.body"
              data-testid="article-body"
              type="textarea"
              :rows="18"
              resize="vertical"
            />
            <div class="markdown-preview-card">
              <div class="markdown-preview-heading">
                {{ transformI18n("articles.editor.preview") }}
              </div>
              <div
                ref="previewElement"
                data-testid="article-markdown-preview"
                class="markdown-preview"
                v-html="previewHtml"
              />
            </div>
          </div>
        </el-form-item>
      </el-card>

      <el-card shadow="never" class="editor-card settings-card">
        <template #header>
          <h2>{{ transformI18n("articles.editor.settingsSection") }}</h2>
        </template>
        <el-form-item :label="transformI18n('articles.editor.status')">
          <el-select v-model="form.status" class="full-width">
            <el-option
              v-for="status in (['DRAFT', 'PUBLISHED', 'PRIVATE', 'PASSWORD', 'SCHEDULED'] as ArticleStatus[])"
              :key="status"
              :label="transformI18n(statusTranslationKey(status))"
              :value="status"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="transformI18n('articles.editor.homepageSlot')">
          <el-select
            v-model="form.homepageSlot"
            data-testid="article-homepage-slot"
            class="full-width"
          >
            <el-option
              v-for="slot in homepageSlotOptions"
              :key="slot"
              :label="transformI18n(homepageSlotTranslationKey(slot))"
              :value="slot"
              :disabled="homepageSlotDisabled(slot)"
            />
          </el-select>
          <p class="field-hint">
            {{ transformI18n("articles.editor.homepageSlotHint") }}
          </p>
        </el-form-item>
        <el-form-item
          :label="transformI18n('articles.editor.slug')"
          :error="fieldError('slug')"
        >
          <el-input
            v-model="form.slug"
            maxlength="160"
            placeholder="article-slug"
            @blur="form.slug = form.slug.trim().toLowerCase()"
          />
          <p class="field-hint">
            {{ transformI18n("articles.editor.slugHint") }}
          </p>
        </el-form-item>
        <el-form-item
          :label="transformI18n('articles.editor.category')"
          :error="fieldError('categoryId')"
        >
          <el-select v-model="form.categoryId" clearable class="full-width">
            <el-option
              v-for="category in categories"
              :key="category.id"
              :label="dictionaryName(category)"
              :value="category.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          :label="transformI18n('articles.editor.tags')"
          :error="fieldError('tagIds')"
        >
          <el-select
            v-model="form.tagIds"
            multiple
            :multiple-limit="20"
            clearable
            class="full-width"
          >
            <el-option
              v-for="tag in tags"
              :key="tag.id"
              :label="dictionaryName(tag)"
              :value="tag.id"
            />
          </el-select>
          <p class="field-hint">
            {{ transformI18n("articles.editor.tagLimit") }}
            {{ form.tagIds.length }}/20
          </p>
        </el-form-item>
        <el-form-item :label="transformI18n('articles.editor.cover')">
          <div class="cover-selector">
            <div v-if="form.coverAttachmentId" class="cover-preview">
              <el-image
                v-if="form.coverUrl"
                class="cover-image"
                :src="form.coverUrl"
                fit="cover"
              />
              <div class="cover-meta">
                <strong>{{ transformI18n("articles.editor.coverSelected") }}</strong>
                <span>#{{ form.coverAttachmentId }}</span>
                <a
                  v-if="form.coverUrl"
                  :href="form.coverUrl"
                  target="_blank"
                  rel="noreferrer"
                >
                  {{ form.coverUrl }}
                </a>
              </div>
              <p class="field-hint">
                {{ transformI18n("articles.editor.coverActiveHint") }}
              </p>
            </div>
            <p v-else class="field-hint">
              {{ transformI18n("articles.editor.coverEmpty") }}
            </p>
            <div class="cover-actions">
              <el-button
                data-testid="article-cover-open-picker"
                type="primary"
                plain
                @click="coverPickerOpen = true"
              >
                {{ transformI18n("articles.editor.coverChoose") }}
              </el-button>
              <el-button
                v-if="form.coverAttachmentId"
                data-testid="article-cover-clear"
                @click="clearCover"
              >
                {{ transformI18n("articles.editor.coverClear") }}
              </el-button>
            </div>
          </div>
        </el-form-item>
        <el-form-item
          v-if="form.status === 'SCHEDULED'"
          :label="transformI18n('articles.editor.publishAt')"
          :error="fieldError('publishAt')"
        >
          <el-date-picker
            v-model="form.publishAt"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            class="full-width"
          />
          <p class="field-hint">
            {{ transformI18n("articles.editor.publishAtHint") }}
          </p>
        </el-form-item>
        <el-form-item
          v-if="form.status === 'PASSWORD'"
          :label="transformI18n('articles.editor.password')"
          :error="fieldError('password')"
        >
          <el-input v-model="form.password" type="password" show-password />
          <p class="field-hint">
            {{
              transformI18n(
                mode === "edit"
                  ? "articles.editor.passwordKeepHint"
                  : "articles.editor.passwordStatusHint"
              )
            }}
          </p>
        </el-form-item>
      </el-card>
    </el-form>
    <AttachmentPickerDialog
      v-model="coverPickerOpen"
      @select="selectCover"
    />
  </section>
</template>

<style scoped lang="scss">
.article-editor-page {
  display: grid;
  gap: 18px;
  padding: 20px;
  background: var(--el-bg-color-page);
}

.editor-toolbar,
.toolbar-actions {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
}

.editor-toolbar h1,
.editor-card h2 {
  margin: 0;
}

.editor-toolbar h1 {
  font-size: 22px;
}

.editor-toolbar p {
  margin: 6px 0 0;
  color: var(--el-text-color-secondary);
}

.editor-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 18px;
  align-items: start;
}

.editor-card {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
}

.editor-card h2 {
  font-size: 17px;
}

.language-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0 16px;
}

.full-width {
  width: 100%;
}

.field-hint {
  margin: 6px 0 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.cover-selector {
  display: grid;
  gap: 10px;
  width: 100%;
}

.cover-preview {
  display: flex;
  gap: 10px;
  align-items: center;
}

.cover-image {
  flex: 0 0 96px;
  width: 96px;
  height: 56px;
  border-radius: 6px;
  background: var(--el-fill-color-light);
}

.cover-meta {
  display: grid;
  gap: 3px;
  min-width: 0;

  span,
  a {
    overflow: hidden;
    text-overflow: ellipsis;
    font-size: 12px;
    color: var(--el-text-color-secondary);
    white-space: nowrap;
  }
}

.cover-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.markdown-workspace {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(280px, 0.8fr);
  gap: 14px;
  width: 100%;
}

.markdown-preview-card {
  min-height: 100%;
  overflow: hidden;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
}

.markdown-preview-heading {
  padding: 10px 12px;
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.markdown-preview {
  min-height: 330px;
  padding: 12px;
  overflow: auto;
  color: var(--el-text-color-primary);

  :deep(h1),
  :deep(h2),
  :deep(h3) {
    margin: 0 0 12px;
  }

  :deep(p),
  :deep(ul),
  :deep(pre) {
    margin: 0 0 12px;
  }

  :deep(code) {
    padding: 2px 4px;
    font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
    background: var(--el-fill-color-light);
    border-radius: 4px;
  }

  :deep(pre) {
    padding: 10px;
    overflow: auto;
    background: var(--el-fill-color-light);
    border-radius: 6px;
  }

  :deep(pre code) {
    padding: 0;
    background: transparent;
  }

  :deep(pre.code-block) {
    position: relative;
    padding-top: 30px;
  }

  :deep(pre.code-block::before) {
    position: absolute;
    top: 10px;
    right: 12px;
    color: var(--el-text-color-secondary);
    content: attr(data-language);
    font-size: 11px;
    line-height: 1;
    text-transform: uppercase;
  }

  :deep(.hljs-comment),
  :deep(.hljs-quote) {
    color: var(--el-text-color-secondary);
  }

  :deep(.hljs-keyword),
  :deep(.hljs-selector-tag),
  :deep(.hljs-literal) {
    color: #a855f7;
  }

  :deep(.hljs-string),
  :deep(.hljs-attr),
  :deep(.hljs-template-variable) {
    color: #3a9d5d;
  }

  :deep(.hljs-number),
  :deep(.hljs-built_in),
  :deep(.hljs-type),
  :deep(.hljs-title.class_) {
    color: #b7791f;
  }

  :deep(.hljs-title.function_),
  :deep(.hljs-property),
  :deep(.hljs-variable) {
    color: #3182ce;
  }

  :deep(pre.mermaid) {
    padding: 12px;
    overflow-x: auto;
    text-align: center;
  }

  :deep(pre.mermaid svg) {
    display: inline-block;
    max-width: none;
    height: auto;
  }

  :deep(.katex-display) {
    overflow-x: auto;
    overflow-y: hidden;
  }

  :deep(.footnotes) {
    padding-top: 10px;
    border-top: 1px solid var(--el-border-color-lighter);
    font-size: 0.9em;
  }

  :deep(li.task-list-item) {
    list-style: none;
  }

  :deep(.task-list-item-checkbox) {
    margin-right: 6px;
  }
}

@media (width <= 1000px) {
  .editor-grid,
  .language-grid,
  .markdown-workspace {
    grid-template-columns: 1fr;
  }
}

@media (width <= 640px) {
  .article-editor-page {
    padding: 12px;
  }

  .editor-toolbar {
    align-items: flex-start;
  }

  .editor-toolbar,
  .toolbar-actions {
    flex-wrap: wrap;
  }
}
</style>
