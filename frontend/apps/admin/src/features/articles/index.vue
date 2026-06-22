<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { ElMessageBox } from "element-plus";
import { useRouter } from "vue-router";
import { i18n, transformI18n } from "@/plugins/i18n";
import { useUserStoreHook } from "@/store/modules/user";
import type { AdminLocale, ArticleListItem, TagItem } from "./model";
import {
  formatJstDateTime,
  localizedName,
  statusTranslationKey
} from "./presentation";
import { useArticleList } from "./useArticleList";

defineOptions({ name: "ArticleList" });

const filtersExpanded = ref(true);
const router = useRouter();
const userStore = useUserStoreHook();
const isAdmin = computed(() => userStore.isAdmin);
const state = useArticleList();
const {
  filters,
  items,
  categories,
  tags,
  total,
  loading,
  error,
  operationError,
  deletingId,
  initialize,
  search,
  reset,
  refresh,
  changePage
} = state;

const locale = computed(
  () => (i18n.global.locale as unknown as { value: AdminLocale }).value
);

function articleTitle(item: ArticleListItem): string {
  return localizedName(
    {
      nameZh: item.titleZh,
      nameJa: item.titleJa,
      nameEn: item.titleEn
    },
    locale.value
  );
}

function languageBadges(item: ArticleListItem): string[] {
  const available = {
    zh: item.titleZh ? "ZH" : null,
    ja: item.titleJa ? "JA" : null,
    en: item.titleEn ? "EN" : null
  };
  return Object.entries(available)
    .filter(([key, value]) => key !== locale.value && value !== null)
    .map(([, value]) => value as string);
}

function categoryName(item: ArticleListItem): string {
  const category = categories.value.find(value => value.id === item.categoryId);
  if (category) return localizedName(category, locale.value);
  return item.categoryNameZh?.trim() || "—";
}

function tagNames(item: ArticleListItem): string[] {
  return item.tagIds
    .map(id => tags.value.find(tag => tag.id === id))
    .filter((tag): tag is TagItem => Boolean(tag))
    .map(tag => localizedName(tag, locale.value));
}

function statusTagType(status: string) {
  if (status === "PUBLISHED") return "success";
  if (status === "PASSWORD") return "warning";
  if (status === "SCHEDULED") return "primary";
  if (status === "PRIVATE") return "danger";
  return "info";
}

function createArticle(): void {
  router.push("/articles/new");
}

function editArticle(id: string): void {
  router.push(`/articles/${id}/edit`);
}

async function confirmRemove(item: ArticleListItem): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `${transformI18n("articles.delete.confirm")}：${articleTitle(item)}`,
      transformI18n("articles.actions.delete"),
      { type: "warning" }
    );
  } catch {
    return;
  }
  await state.remove(item.id);
}

onMounted(initialize);
</script>

<template>
  <section class="article-page">
    <el-card
      data-testid="article-filter-card"
      class="workspace-card filter-card"
      shadow="never"
    >
      <template #header>
        <div class="card-heading">
          <div>
            <h2>{{ transformI18n("articles.filter.title") }}</h2>
          </div>
          <el-button
            text
            type="primary"
            @click="filtersExpanded = !filtersExpanded"
          >
            {{
              transformI18n(
                filtersExpanded
                  ? "articles.filter.collapse"
                  : "articles.filter.expand"
              )
            }}
          </el-button>
        </div>
      </template>

      <div v-show="filtersExpanded">
        <el-form :model="filters" label-position="top" class="filter-grid">
          <el-form-item :label="transformI18n('articles.filter.titleKeyword')">
            <el-input
              v-model="filters.titleKeyword"
              data-testid="title-filter"
              clearable
              :placeholder="transformI18n('articles.filter.titlePlaceholder')"
              @keyup.enter="search"
            />
          </el-form-item>
          <el-form-item :label="transformI18n('articles.filter.status')">
            <el-select
              v-model="filters.status"
              data-testid="status-filter"
              :placeholder="transformI18n('articles.filter.statusPlaceholder')"
            >
              <el-option
                v-for="status in [
                  'ALL',
                  'DRAFT',
                  'PUBLISHED',
                  'PRIVATE',
                  'PASSWORD',
                  'SCHEDULED'
                ]"
                :key="status"
                :label="
                  transformI18n(
                    status === 'ALL'
                      ? 'articles.status.all'
                      : statusTranslationKey(status)
                  )
                "
                :value="status"
              />
            </el-select>
          </el-form-item>
        </el-form>

        <div class="filter-actions">
          <el-button
            data-testid="article-search"
            type="primary"
            @click="search"
          >
            {{ transformI18n("articles.actions.search") }}
          </el-button>
          <el-button data-testid="article-reset" @click="reset">
            {{ transformI18n("articles.actions.reset") }}
          </el-button>
        </div>
        <p class="future-filter-note">
          {{ transformI18n("articles.filter.futureHint") }}
        </p>
      </div>
    </el-card>

    <el-card
      data-testid="article-result-card"
      class="workspace-card result-card"
      shadow="never"
    >
      <template #header>
        <div class="card-heading result-heading">
          <div>
            <h2>
              {{ transformI18n("articles.result.total") }}
              <span class="result-count">{{ total }}</span>
            </h2>
          </div>
          <div class="result-actions">
            <el-button
              v-if="isAdmin"
              data-testid="article-create"
              type="primary"
              @click="createArticle"
            >
              {{ transformI18n("articles.actions.create") }}
            </el-button>
            <el-button
              data-testid="article-refresh"
              :loading="loading"
              @click="refresh"
            >
              {{ transformI18n("articles.actions.refresh") }}
            </el-button>
          </div>
        </div>
      </template>

      <el-alert
        v-if="operationError"
        data-testid="article-operation-error"
        type="error"
        :closable="false"
        :title="transformI18n('articles.delete.error')"
        show-icon
      />

      <el-skeleton
        v-if="loading && items.length === 0"
        data-testid="article-loading"
        :rows="7"
        animated
      />

      <el-alert
        v-else-if="error"
        data-testid="article-error"
        type="error"
        :closable="false"
        :title="transformI18n('articles.states.error')"
        show-icon
      >
        <el-button
          data-testid="article-retry"
          type="primary"
          link
          @click="refresh"
        >
          {{ transformI18n("articles.actions.retry") }}
        </el-button>
      </el-alert>

      <el-empty
        v-else-if="items.length === 0"
        data-testid="article-empty"
        :description="transformI18n('articles.states.empty')"
      />

      <template v-else>
        <div class="table-scroll">
          <el-table :data="items" row-key="id" class="article-table">
            <el-table-column
              :label="transformI18n('articles.columns.title')"
              min-width="340"
            >
              <template #default="{ row }">
                <div class="article-title-cell">
                  <div class="article-title-line">
                    <strong>{{ articleTitle(row) }}</strong>
                    <el-tag
                      v-for="badge in languageBadges(row)"
                      :key="badge"
                      size="small"
                      effect="plain"
                    >
                      {{ badge }}
                    </el-tag>
                  </div>
                  <span>/articles/{{ row.slug }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column
              :label="transformI18n('articles.columns.status')"
              width="105"
            >
              <template #default="{ row }">
                <el-tag :type="statusTagType(row.status)" effect="light">
                  {{ transformI18n(statusTranslationKey(row.status)) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column
              :label="transformI18n('articles.columns.category')"
              min-width="120"
            >
              <template #default="{ row }">{{ categoryName(row) }}</template>
            </el-table-column>
            <el-table-column
              :label="transformI18n('articles.columns.tags')"
              min-width="180"
            >
              <template #default="{ row }">
                <div v-if="tagNames(row).length" class="tag-list">
                  <el-tag
                    v-for="tag in tagNames(row)"
                    :key="tag"
                    size="small"
                    effect="light"
                  >
                    {{ tag }}
                  </el-tag>
                </div>
                <span v-else>—</span>
              </template>
            </el-table-column>
            <el-table-column
              prop="commentCount"
              :label="transformI18n('articles.columns.comments')"
              width="90"
            />
            <el-table-column
              :label="transformI18n('articles.columns.publishAt')"
              width="155"
            >
              <template #default="{ row }">{{
                formatJstDateTime(row.publishAt)
              }}</template>
            </el-table-column>
            <el-table-column
              :label="transformI18n('articles.columns.updatedAt')"
              width="155"
            >
              <template #default="{ row }">{{
                formatJstDateTime(row.updatedAt)
              }}</template>
            </el-table-column>
            <el-table-column
              v-if="isAdmin"
              data-testid="article-operation-column"
              :label="transformI18n('articles.columns.operations')"
              fixed="right"
              width="150"
            >
              <template #default="{ row }">
                <el-button link type="primary" @click="editArticle(row.id)">
                  {{ transformI18n("articles.actions.edit") }}
                </el-button>
                <el-button
                  :data-testid="`article-delete-${row.id}`"
                  link
                  type="danger"
                  :loading="deletingId === row.id"
                  :disabled="deletingId !== null"
                  @click="confirmRemove(row)"
                >
                  {{ transformI18n("articles.actions.delete") }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <el-pagination
          class="article-pagination"
          background
          layout="total, sizes, prev, pager, next"
          :current-page="filters.page"
          :page-size="filters.size"
          :page-sizes="[10, 20, 50]"
          :total="total"
          @current-change="page => changePage(page)"
          @size-change="size => changePage(1, size)"
        />
      </template>
    </el-card>
  </section>
</template>

<style scoped lang="scss">
.article-page {
  display: grid;
  gap: 18px;
  padding: 20px;
  color: var(--el-text-color-primary);
  background: var(--el-bg-color-page);
}

.workspace-card {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;

  :deep(.el-card__header) {
    padding: 14px 20px;
  }
}

.card-heading {
  display: flex;
  gap: 16px;
  align-items: center;
  justify-content: space-between;

  h2 {
    margin: 0;
    font-size: 18px;
    font-weight: 600;
  }
}

.filter-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(260px, 0.7fr);
  gap: 28px;
}

.filter-actions {
  display: flex;
  gap: 10px;
}

.result-actions {
  display: flex;
  gap: 10px;
}

.future-filter-note {
  padding-top: 14px;
  margin: 18px 0 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  border-top: 1px dashed var(--el-border-color-lighter);
}

.result-count {
  font-variant-numeric: tabular-nums;
  color: var(--el-color-primary);
}

.table-scroll {
  overflow-x: auto;
}

.article-table {
  min-width: 1120px;

  :deep(.el-table__cell) {
    padding: 5px 0;
  }
}

.article-title-cell {
  display: grid;
  gap: 3px;

  > span {
    overflow: hidden;
    text-overflow: ellipsis;
    font-size: 12px;
    color: var(--el-text-color-secondary);
    white-space: nowrap;
  }
}

.article-title-line,
.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}

.article-title-line {
  flex-wrap: nowrap;

  strong {
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  :deep(.el-tag) {
    flex: 0 0 auto;
  }
}

.article-pagination {
  justify-content: flex-end;
  margin-top: 18px;
}

@media (width <= 760px) {
  .article-page {
    padding: 12px;
  }

  .filter-grid {
    grid-template-columns: 1fr;
    gap: 0;
  }

  .card-heading {
    align-items: flex-start;
  }

  .article-pagination {
    justify-content: flex-start;
    overflow-x: auto;
  }
}
</style>
