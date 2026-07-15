<script setup lang="ts">
import { computed, onMounted } from "vue";
import { ElMessageBox } from "element-plus";
import { i18n, transformI18n } from "@/plugins/i18n";
import { useUserStoreHook } from "@/store/modules/user";
import { message } from "@/utils/message";
import type { AdminLocale } from "@/features/articles/model";
import {
  formatJstDateTime,
  localizedName
} from "@/features/articles/presentation";
import type { CategoryItem } from "../model";
import { normalizeSlug } from "../form";
import { useCategoryManagement } from "./useCategoryManagement";

defineOptions({ name: "CategoryManagement" });

const userStore = useUserStoreHook();
const isAdmin = computed(() => userStore.isAdmin);
const locale = computed(
  () => (i18n.global.locale as unknown as { value: AdminLocale }).value
);
const state = useCategoryManagement();
const {
  filteredItems,
  keyword,
  loading,
  requestError,
  operationError,
  dialogOpen,
  editingId,
  form,
  formErrors,
  saving,
  sortDrafts,
  dirtySortItems,
  initialize,
  retry,
  openCreate,
  openEdit,
  closeDialog,
  save,
  setSortOrder,
  saveSortOrders
} = state;

function languageBadges(item: CategoryItem): string[] {
  const names = { zh: item.nameZh, ja: item.nameJa, en: item.nameEn };
  return Object.entries(names)
    .filter(([key, value]) => key !== locale.value && Boolean(value?.trim()))
    .map(([key]) => key.toUpperCase());
}

function fieldError(field: keyof typeof form): string {
  const code = formErrors[field];
  return code ? transformI18n(`taxonomy.validation.${code}`) : "";
}

function normalizeFormSlug(): void {
  form.slug = normalizeSlug(form.slug);
}

const operationErrorKey = computed(() => {
  if (!operationError.value) return "";
  if (operationError.value.kind === "conflict") {
    return operationError.value.action === "delete"
      ? "taxonomy.errors.deleteConflict"
      : "taxonomy.errors.saveConflict";
  }
  if (operationError.value.kind === "forbidden") {
    return "taxonomy.errors.forbidden";
  }
  return "taxonomy.errors.request";
});

async function confirmRemove(id: string): Promise<void> {
  await ElMessageBox.confirm(
    transformI18n("taxonomy.categories.deleteConfirm"),
    transformI18n("taxonomy.actions.delete"),
    { type: "warning" }
  );
  if (await state.remove(id)) {
    message(transformI18n("taxonomy.feedback.deleted"), { type: "success" });
  }
}

async function submitSave(): Promise<void> {
  if (await save()) {
    message(transformI18n("taxonomy.feedback.saved"), { type: "success" });
  }
}

async function submitSort(): Promise<void> {
  if (await saveSortOrders()) {
    message(transformI18n("taxonomy.feedback.sortSaved"), { type: "success" });
  }
}

onMounted(initialize);

defineExpose({ state, confirmRemove });
</script>

<template>
  <section class="taxonomy-page">
    <el-card
      data-testid="category-filter-card"
      class="workspace-card compact-filter-card"
      shadow="never"
    >
      <template #header>
        <h2>{{ transformI18n("taxonomy.filter.title") }}</h2>
      </template>
      <el-form label-position="top" class="taxonomy-filter-form">
        <el-form-item :label="transformI18n('taxonomy.filter.keyword')">
          <el-input
            v-model="keyword"
            clearable
            :placeholder="transformI18n('taxonomy.filter.placeholder')"
          />
        </el-form-item>
      </el-form>
    </el-card>

    <el-card
      data-testid="category-result-card"
      class="workspace-card"
      shadow="never"
    >
      <template #header>
        <div class="card-heading">
          <h2>
            {{ transformI18n("taxonomy.categories.title") }}
            <span>{{ filteredItems.length }}</span>
          </h2>
          <div v-if="isAdmin" class="actions">
            <el-button
              data-testid="category-save-sort"
              :disabled="dirtySortItems.length === 0"
              @click="submitSort"
            >
              {{ transformI18n("taxonomy.actions.saveSort") }}
            </el-button>
            <el-button
              data-testid="category-create"
              type="primary"
              @click="openCreate"
            >
              {{ transformI18n("taxonomy.categories.create") }}
            </el-button>
          </div>
        </div>
      </template>

      <el-alert
        v-if="operationErrorKey"
        type="error"
        :closable="false"
        :title="transformI18n(operationErrorKey)"
      />
      <p v-if="isAdmin" class="field-hint">
        {{ transformI18n("taxonomy.sortHint") }}
      </p>
      <el-skeleton v-if="loading && !filteredItems.length" :rows="6" animated />
      <el-alert
        v-else-if="requestError"
        data-testid="category-error"
        type="error"
        :closable="false"
        :title="transformI18n('taxonomy.states.error')"
      >
        <el-button link type="primary" @click="retry">
          {{ transformI18n("taxonomy.actions.retry") }}
        </el-button>
      </el-alert>
      <el-empty
        v-else-if="!filteredItems.length"
        data-testid="category-empty"
        :description="transformI18n('taxonomy.categories.empty')"
      />
      <el-table v-else :data="filteredItems" row-key="id">
        <el-table-column :label="transformI18n('taxonomy.columns.name')" min-width="220">
          <template #default="{ row }">
            <div class="name-cell">
              <strong>{{ localizedName(row, locale) }}</strong>
              <el-tag
                v-for="badge in languageBadges(row)"
                :key="badge"
                size="small"
                effect="plain"
              >{{ badge }}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="slug" label="Slug" min-width="180" />
        <el-table-column :label="transformI18n('taxonomy.columns.sortOrder')" width="150">
          <template #default="{ row }">
            <el-input-number
              v-if="isAdmin"
              :model-value="sortDrafts[row.id]"
              :min="0"
              :max="1000000"
              controls-position="right"
              @change="value => setSortOrder(row.id, Number(value))"
            />
            <span v-else>{{ row.sortOrder }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="transformI18n('taxonomy.columns.updatedAt')" width="170">
          <template #default="{ row }">{{ formatJstDateTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column
          v-if="isAdmin"
          data-testid="category-operation-column"
          :label="transformI18n('taxonomy.columns.operations')"
          fixed="right"
          width="170"
        >
          <template #default="{ row }">
            <el-button size="small" plain type="primary" @click="openEdit(row)">
              {{ transformI18n("taxonomy.actions.edit") }}
            </el-button>
            <el-button size="small" plain type="danger" @click="confirmRemove(row.id)">
              {{ transformI18n("taxonomy.actions.delete") }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="dialogOpen"
      :title="transformI18n(editingId ? 'taxonomy.categories.edit' : 'taxonomy.categories.create')"
      width="560px"
      destroy-on-close
      @closed="closeDialog"
    >
      <el-form :model="form" label-position="top">
        <el-form-item required :label="transformI18n('taxonomy.fields.nameZh')" :error="fieldError('nameZh')">
          <el-input v-model="form.nameZh" maxlength="64" show-word-limit />
        </el-form-item>
        <el-form-item :label="transformI18n('taxonomy.fields.nameJa')" :error="fieldError('nameJa')">
          <el-input v-model="form.nameJa" maxlength="64" show-word-limit />
        </el-form-item>
        <el-form-item :label="transformI18n('taxonomy.fields.nameEn')" :error="fieldError('nameEn')">
          <el-input v-model="form.nameEn" maxlength="64" show-word-limit />
        </el-form-item>
        <el-form-item required :label="transformI18n('taxonomy.fields.slug')" :error="fieldError('slug')">
          <el-input
            v-model="form.slug"
            data-testid="category-slug-input"
            :disabled="Boolean(editingId)"
            maxlength="64"
            @blur="normalizeFormSlug"
          />
          <p class="field-hint">
            {{ transformI18n("taxonomy.fields.slugLockHint") }}
          </p>
        </el-form-item>
        <el-form-item :label="transformI18n('taxonomy.fields.sortOrder')" :error="fieldError('sortOrder')">
          <el-input-number v-model="form.sortOrder" :min="0" :max="1000000" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeDialog">{{ transformI18n("taxonomy.actions.cancel") }}</el-button>
        <el-button type="primary" :loading="saving" @click="submitSave">
          {{ transformI18n("taxonomy.actions.save") }}
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped lang="scss">
.taxonomy-page {
  display: grid;
  gap: 16px;
  padding: 20px 24px;
  background: var(--el-bg-color-page);
}

.workspace-card {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
}

.compact-filter-card {
  width: min(100%, 460px);
}

.taxonomy-filter-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.card-heading,
.actions,
.name-cell {
  display: flex;
  gap: 10px;
  align-items: center;
}

.card-heading {
  justify-content: space-between;
}

h2 {
  margin: 0;
  font-size: 18px;
}

.card-heading h2 span {
  color: var(--el-color-primary);
}

.field-hint {
  margin: 6px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.5;
}

@media (width <= 760px) {
  .taxonomy-page {
    padding: 12px;
  }
}
</style>
