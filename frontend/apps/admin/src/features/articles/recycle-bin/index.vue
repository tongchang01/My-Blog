<script setup lang="ts">
import { computed, onMounted } from "vue";
import { ElMessageBox } from "element-plus";
import { i18n, transformI18n } from "@/plugins/i18n";
import { useUserStoreHook } from "@/store/modules/user";
import type {
  AdminLocale,
  DeletedArticleListItem
} from "../model";
import {
  formatJstDateTime,
  localizedName,
  statusTranslationKey
} from "../presentation";
import { useArticleRecycleBin } from "./useArticleRecycleBin";

defineOptions({ name: "ArticleRecycleBin" });

const userStore = useUserStoreHook();
const isAdmin = computed(() => userStore.isAdmin);
const locale = computed(
  () => (i18n.global.locale as unknown as { value: AdminLocale }).value
);
const state = useArticleRecycleBin();
const {
  items,
  categories,
  page,
  size,
  total,
  loading,
  error,
  operationError,
  restoringId,
  initialize,
  retry,
  refresh,
  changePage
} = state;

function articleTitle(item: DeletedArticleListItem): string {
  return localizedName(
    {
      nameZh: item.titleZh,
      nameJa: item.titleJa,
      nameEn: item.titleEn
    },
    locale.value
  );
}

function categoryName(item: DeletedArticleListItem): string {
  const category = categories.value.find(value => value.id === item.categoryId);
  return category ? localizedName(category, locale.value) : "—";
}

function statusTagType(status: string) {
  if (status === "PUBLISHED") return "success";
  if (status === "PASSWORD") return "warning";
  if (status === "SCHEDULED") return "primary";
  if (status === "PRIVATE") return "danger";
  return "info";
}

const operationErrorKey = computed(() => {
  if (!operationError.value) return "";
  if (operationError.value.kind === "conflict") {
    return "articles.recycle.errors.referenceConflict";
  }
  if (operationError.value.kind === "notFound") {
    return "articles.recycle.errors.notFound";
  }
  if (operationError.value.kind === "forbidden") {
    return "articles.recycle.errors.forbidden";
  }
  return "articles.recycle.errors.request";
});

async function confirmRestore(item: DeletedArticleListItem): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `${transformI18n("articles.recycle.restoreConfirm")}：${articleTitle(item)}`,
      transformI18n("articles.recycle.restore"),
      { type: "warning" }
    );
  } catch {
    return;
  }
  await state.restore(item.id);
}

onMounted(initialize);
</script>

<template>
  <section class="recycle-page">
    <el-card
      data-testid="article-recycle-card"
      class="workspace-card"
      shadow="never"
    >
      <template #header>
        <div class="card-heading">
          <h2>
            {{ transformI18n("articles.recycle.title") }}
            <span class="result-count">{{ total }}</span>
          </h2>
          <el-button :loading="loading" @click="refresh">
            {{ transformI18n("articles.actions.refresh") }}
          </el-button>
        </div>
      </template>

      <el-alert
        v-if="operationErrorKey"
        data-testid="article-recycle-operation-error"
        type="error"
        :closable="false"
        :title="transformI18n(operationErrorKey)"
        show-icon
      />

      <el-skeleton
        v-if="loading && items.length === 0"
        data-testid="article-recycle-loading"
        :rows="7"
        animated
      />
      <el-alert
        v-else-if="error"
        data-testid="article-recycle-error"
        type="error"
        :closable="false"
        :title="transformI18n('articles.recycle.errors.load')"
        show-icon
      >
        <el-button type="primary" link @click="retry">
          {{ transformI18n("articles.actions.retry") }}
        </el-button>
      </el-alert>
      <el-empty
        v-else-if="items.length === 0"
        data-testid="article-recycle-empty"
        :description="transformI18n('articles.recycle.empty')"
      />
      <template v-else>
        <div class="table-scroll">
          <el-table :data="items" row-key="id" class="recycle-table">
            <el-table-column
              :label="transformI18n('articles.columns.title')"
              min-width="300"
            >
              <template #default="{ row }">
                <strong>{{ articleTitle(row) }}</strong>
              </template>
            </el-table-column>
            <el-table-column
              :label="transformI18n('articles.columns.status')"
              width="110"
            >
              <template #default="{ row }">
                <el-tag :type="statusTagType(row.status)" effect="light">
                  {{ transformI18n(statusTranslationKey(row.status)) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column
              :label="transformI18n('articles.columns.category')"
              min-width="140"
            >
              <template #default="{ row }">{{ categoryName(row) }}</template>
            </el-table-column>
            <el-table-column
              :label="transformI18n('articles.recycle.deletedAt')"
              width="170"
            >
              <template #default="{ row }">
                {{ formatJstDateTime(row.deletedAt) }}
              </template>
            </el-table-column>
            <el-table-column
              prop="deletedBy"
              :label="transformI18n('articles.recycle.deletedBy')"
              min-width="170"
            />
            <el-table-column
              v-if="isAdmin"
              data-testid="article-operation-column"
              :label="transformI18n('articles.columns.operations')"
              fixed="right"
              width="100"
            >
              <template #default="{ row }">
                <el-button
                  :data-testid="`article-restore-${row.id}`"
                  link
                  type="primary"
                  :loading="restoringId === row.id"
                  :disabled="restoringId !== null"
                  @click="confirmRestore(row)"
                >
                  {{ transformI18n("articles.recycle.restore") }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <el-pagination
          class="recycle-pagination"
          background
          layout="total, sizes, prev, pager, next"
          :current-page="page"
          :page-size="size"
          :page-sizes="[10, 20, 50]"
          :total="total"
          @current-change="nextPage => changePage(nextPage)"
          @size-change="nextSize => changePage(1, nextSize)"
        />
      </template>
    </el-card>
  </section>
</template>

<style scoped lang="scss">
.recycle-page {
  padding: 20px;
  color: var(--el-text-color-primary);
  background: var(--el-bg-color-page);
}

.workspace-card {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
}

.card-heading {
  display: flex;
  gap: 16px;
  align-items: center;
  justify-content: space-between;

  h2 {
    margin: 0;
    font-size: 18px;
  }
}

.result-count {
  margin-left: 6px;
  color: var(--el-color-primary);
}

.table-scroll {
  overflow-x: auto;
}

.recycle-table {
  min-width: 960px;
}

.recycle-pagination {
  justify-content: flex-end;
  margin-top: 18px;
}

@media (width <= 760px) {
  .recycle-page {
    padding: 12px;
  }

  .recycle-pagination {
    justify-content: flex-start;
    overflow-x: auto;
  }
}
</style>
