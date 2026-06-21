<script setup lang="ts">
import { computed, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { i18n, transformI18n } from "@/plugins/i18n";
import type { AdminLocale, ArticleStatus, LocalizedNames } from "../model";
import { localizedName, statusTranslationKey } from "../presentation";
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
const pageTitle = computed(() =>
  transformI18n(
    mode === "edit" ? "articles.editor.editTitle" : "articles.editor.createTitle"
  )
);

function dictionaryName(item: LocalizedNames): string {
  return localizedName(item, locale.value);
}

function fieldError(field: keyof typeof form): string {
  const code = errors.value[field];
  return code ? transformI18n(`articles.editor.validation.${code}`) : "";
}

async function submit(): Promise<void> {
  try {
    const result = await save();
    if (result) await router.push("/articles/list");
  } catch {
    // 请求错误由页面中的 alert 展示，表单数据保持不变。
  }
}

onMounted(() => initialize().catch(() => undefined));
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
            <el-input v-model="form.titleZh" maxlength="200" show-word-limit />
          </el-form-item>
          <el-form-item :label="transformI18n('articles.editor.titleJa')">
            <el-input v-model="form.titleJa" maxlength="200" show-word-limit />
          </el-form-item>
          <el-form-item :label="transformI18n('articles.editor.titleEn')">
            <el-input v-model="form.titleEn" maxlength="200" show-word-limit />
          </el-form-item>
          <el-form-item
            :label="transformI18n('articles.editor.summaryZh')"
            :error="fieldError('summaryZh')"
          >
            <el-input v-model="form.summaryZh" type="textarea" :rows="3" />
          </el-form-item>
          <el-form-item :label="transformI18n('articles.editor.summaryJa')">
            <el-input v-model="form.summaryJa" type="textarea" :rows="3" />
          </el-form-item>
          <el-form-item :label="transformI18n('articles.editor.summaryEn')">
            <el-input v-model="form.summaryEn" type="textarea" :rows="3" />
          </el-form-item>
        </div>
        <el-form-item
          :label="transformI18n('articles.editor.body')"
          :error="fieldError('body')"
        >
          <el-input
            v-model="form.body"
            data-testid="article-body"
            type="textarea"
            :rows="18"
            resize="vertical"
          />
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
        <el-form-item :label="transformI18n('articles.editor.slug')">
          <el-input v-model="form.slug" placeholder="article-slug" />
        </el-form-item>
        <el-form-item :label="transformI18n('articles.editor.category')">
          <el-select v-model="form.categoryId" clearable class="full-width">
            <el-option
              v-for="category in categories"
              :key="category.id"
              :label="dictionaryName(category)"
              :value="category.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="transformI18n('articles.editor.tags')">
          <el-select v-model="form.tagIds" multiple clearable class="full-width">
            <el-option
              v-for="tag in tags"
              :key="tag.id"
              :label="dictionaryName(tag)"
              :value="tag.id"
            />
          </el-select>
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
        </el-form-item>
        <el-form-item
          v-if="form.status === 'PASSWORD'"
          :label="transformI18n('articles.editor.password')"
          :error="fieldError('password')"
        >
          <el-input v-model="form.password" type="password" show-password />
          <p v-if="mode === 'edit'" class="field-hint">
            {{ transformI18n("articles.editor.passwordKeepHint") }}
          </p>
        </el-form-item>
      </el-card>
    </el-form>
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

@media (width <= 1000px) {
  .editor-grid,
  .language-grid {
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
