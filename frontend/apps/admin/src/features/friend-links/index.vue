<script setup lang="ts">
import { computed, onMounted } from "vue";
import { ElMessageBox } from "element-plus";
import { transformI18n } from "@/plugins/i18n";
import { useUserStoreHook } from "@/store/modules/user";
import { formatJstDateTime } from "@/features/articles/presentation";
import type { FriendLinkItem, FriendLinkStatus } from "./model";
import { useFriendLinkManagement } from "./useFriendLinkManagement";

defineOptions({ name: "FriendLinkManagement" });

const userStore = useUserStoreHook();
const isAdmin = computed(() => userStore.isAdmin);
const state = useFriendLinkManagement();
const {
  filters,
  items,
  total,
  loading,
  error,
  operationError,
  operatingId,
  dialogOpen,
  editingId,
  form,
  formErrors,
  saving,
  sortDrafts,
  dirtySortItems,
  initialize,
  search,
  reset,
  refresh,
  changePage,
  openCreate,
  openEdit,
  closeDialog,
  save,
  setSortOrder,
  saveSortOrders
} = state;

const statuses: Array<FriendLinkStatus | "ALL"> = [
  "ALL",
  "VISIBLE",
  "HIDDEN"
];

function statusKey(status: FriendLinkStatus | "ALL"): string {
  if (status === "VISIBLE") return "friendLinks.status.visible";
  if (status === "HIDDEN") return "friendLinks.status.hidden";
  return "friendLinks.status.all";
}

function statusTagType(status: FriendLinkStatus) {
  return status === "VISIBLE" ? "success" : "info";
}

function nextStatus(item: FriendLinkItem): FriendLinkStatus {
  return item.status === "VISIBLE" ? "HIDDEN" : "VISIBLE";
}

function statusActionKey(item: FriendLinkItem): string {
  return item.status === "VISIBLE"
    ? "friendLinks.actions.hide"
    : "friendLinks.actions.show";
}

function statusButtonTestId(item: FriendLinkItem): string {
  return item.status === "VISIBLE"
    ? `friend-link-hide-${item.id}`
    : `friend-link-show-${item.id}`;
}

function fieldError(field: keyof typeof form): string {
  const code = formErrors[field];
  return code ? transformI18n(`friendLinks.validation.${code}`) : "";
}

async function confirmStatus(item: FriendLinkItem): Promise<void> {
  try {
    await ElMessageBox.confirm(
      transformI18n("friendLinks.actions.statusConfirm"),
      transformI18n("friendLinks.actions.confirmTitle"),
      { type: "warning" }
    );
  } catch {
    return;
  }
  await state.updateStatus(item.id, nextStatus(item));
}

async function confirmRemove(item: FriendLinkItem): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `${transformI18n("friendLinks.actions.deleteConfirm")}：${item.name}`,
      transformI18n("friendLinks.actions.confirmTitle"),
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
  <section class="friend-link-page">
    <el-card
      data-testid="friend-link-filter-card"
      class="workspace-card filter-card"
      shadow="never"
    >
      <template #header>
        <div class="card-heading">
          <h2>{{ transformI18n("friendLinks.filter.title") }}</h2>
        </div>
      </template>

      <el-form :model="filters" label-position="top" class="filter-grid">
        <el-form-item :label="transformI18n('friendLinks.filter.keyword')">
          <el-input
            v-model="filters.keyword"
            data-testid="friend-link-keyword"
            clearable
            :placeholder="transformI18n('friendLinks.filter.keywordPlaceholder')"
            @keyup.enter="search"
          />
        </el-form-item>
        <el-form-item :label="transformI18n('friendLinks.filter.status')">
          <el-select
            v-model="filters.status"
            data-testid="friend-link-status"
          >
            <el-option
              v-for="status in statuses"
              :key="status"
              :label="transformI18n(statusKey(status))"
              :value="status"
            />
          </el-select>
        </el-form-item>
      </el-form>

      <div class="filter-actions">
        <div class="filter-buttons">
          <el-button data-testid="friend-link-search" type="primary" @click="search">
            {{ transformI18n("articles.actions.search") }}
          </el-button>
          <el-button data-testid="friend-link-reset" @click="reset">
            {{ transformI18n("articles.actions.reset") }}
          </el-button>
        </div>
      </div>
    </el-card>

    <el-card
      data-testid="friend-link-result-card"
      class="workspace-card result-card"
      shadow="never"
    >
      <template #header>
        <div class="card-heading result-heading">
          <h2>
            {{ transformI18n("friendLinks.result.total") }}
            <span class="result-count">{{ total }}</span>
          </h2>
          <div class="result-actions">
            <el-button
              data-testid="friend-link-refresh"
              :loading="loading"
              @click="refresh"
            >
              {{ transformI18n("articles.actions.refresh") }}
            </el-button>
            <el-button
              v-if="isAdmin"
              data-testid="friend-link-save-sort"
              :disabled="dirtySortItems.length === 0"
              type="primary"
              @click="saveSortOrders"
            >
              {{ transformI18n("friendLinks.actions.saveSort") }}
            </el-button>
            <el-button
              v-if="isAdmin"
              data-testid="friend-link-create"
              type="primary"
              @click="openCreate"
            >
              {{ transformI18n("friendLinks.actions.create") }}
            </el-button>
          </div>
        </div>
      </template>

      <el-alert
        v-if="operationError"
        data-testid="friend-link-operation-error"
        type="error"
        :closable="false"
        :title="transformI18n('friendLinks.errors.operation')"
        show-icon
      />

      <el-skeleton
        v-if="loading && items.length === 0"
        data-testid="friend-link-loading"
        :rows="7"
        animated
      />

      <el-alert
        v-else-if="error"
        data-testid="friend-link-error"
        type="error"
        :closable="false"
        :title="transformI18n('friendLinks.errors.load')"
        show-icon
      >
        <el-button
          data-testid="friend-link-retry"
          type="primary"
          link
          @click="refresh"
        >
          {{ transformI18n("articles.actions.retry") }}
        </el-button>
      </el-alert>

      <el-empty
        v-else-if="items.length === 0"
        data-testid="friend-link-empty"
        :description="transformI18n('friendLinks.empty')"
      />

      <template v-else>
        <div class="table-scroll">
          <el-table :data="items" row-key="id" class="friend-link-table">
            <el-table-column
              :label="transformI18n('friendLinks.columns.name')"
              min-width="260"
            >
              <template #default="{ row }">
                <div class="friend-link-name-cell">
                  <el-avatar :src="row.avatarUrl || undefined" :size="36">
                    {{ row.name.slice(0, 1).toUpperCase() }}
                  </el-avatar>
                  <div>
                    <strong>{{ row.name }}</strong>
                    <a :href="row.url" target="_blank" rel="noreferrer">
                      {{ row.url }}
                    </a>
                    <span>#{{ row.id }}</span>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column
              :label="transformI18n('friendLinks.columns.description')"
              min-width="220"
            >
              <template #default="{ row }">
                {{ row.description || "—" }}
              </template>
            </el-table-column>
            <el-table-column
              :label="transformI18n('friendLinks.columns.status')"
              width="110"
            >
              <template #default="{ row }">
                <el-tag :type="statusTagType(row.status)" effect="light">
                  {{ transformI18n(statusKey(row.status)) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column
              :label="transformI18n('friendLinks.columns.sortOrder')"
              width="150"
            >
              <template #default="{ row }">
                <el-input-number
                  v-if="isAdmin"
                  :model-value="sortDrafts[row.id]"
                  :min="0"
                  :max="1000000"
                  :step="1"
                  @update:model-value="value => setSortOrder(row.id, value ?? row.sortOrder)"
                />
                <span v-else>{{ row.sortOrder }}</span>
              </template>
            </el-table-column>
            <el-table-column
              :label="transformI18n('friendLinks.columns.updatedAt')"
              width="155"
            >
              <template #default="{ row }">
                {{ formatJstDateTime(row.updatedAt) }}
              </template>
            </el-table-column>
            <el-table-column
              v-if="isAdmin"
              data-testid="friend-link-operation-column"
              :label="transformI18n('articles.columns.operations')"
              fixed="right"
              width="220"
            >
              <template #default="{ row }">
                <el-button link type="primary" @click="openEdit(row)">
                  {{ transformI18n("articles.actions.edit") }}
                </el-button>
                <el-button
                  :data-testid="statusButtonTestId(row)"
                  link
                  type="warning"
                  :loading="operatingId === row.id"
                  :disabled="operatingId !== null"
                  @click="confirmStatus(row)"
                >
                  {{ transformI18n(statusActionKey(row)) }}
                </el-button>
                <el-button
                  :data-testid="`friend-link-delete-${row.id}`"
                  link
                  type="danger"
                  :loading="operatingId === row.id"
                  :disabled="operatingId !== null"
                  @click="confirmRemove(row)"
                >
                  {{ transformI18n("articles.actions.delete") }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <el-pagination
          class="friend-link-pagination"
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

    <el-dialog
      v-model="dialogOpen"
      :title="transformI18n(editingId ? 'friendLinks.dialog.edit' : 'friendLinks.dialog.create')"
      width="560px"
      @closed="closeDialog"
    >
      <el-form :model="form" label-position="top">
        <el-form-item
          :label="transformI18n('friendLinks.fields.name')"
          :error="fieldError('name')"
        >
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item
          :label="transformI18n('friendLinks.fields.url')"
          :error="fieldError('url')"
        >
          <el-input v-model="form.url" />
        </el-form-item>
        <el-form-item
          :label="transformI18n('friendLinks.fields.avatarUrl')"
          :error="fieldError('avatarUrl')"
        >
          <el-input v-model="form.avatarUrl" />
        </el-form-item>
        <el-form-item :label="transformI18n('friendLinks.fields.description')">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item
          :label="transformI18n('friendLinks.fields.sortOrder')"
          :error="fieldError('sortOrder')"
        >
          <el-input-number
            v-model="form.sortOrder"
            :min="0"
            :max="1000000"
            :step="1"
          />
        </el-form-item>
        <el-form-item :label="transformI18n('friendLinks.fields.status')">
          <el-select v-model="form.status">
            <el-option
              v-for="status in ['VISIBLE', 'HIDDEN']"
              :key="status"
              :label="transformI18n(statusKey(status as FriendLinkStatus))"
              :value="status"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeDialog">
          {{ transformI18n("taxonomy.actions.cancel") }}
        </el-button>
        <el-button type="primary" :loading="saving" @click="save">
          {{ transformI18n("taxonomy.actions.save") }}
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped lang="scss">
.friend-link-page {
  display: grid;
  gap: 18px;
  padding: 20px;
  color: var(--el-text-color-primary);
  background: var(--el-bg-color-page);
}

.workspace-card {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
}

.card-heading,
.filter-actions,
.result-heading,
.result-actions {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
}

.card-heading h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.filter-buttons {
  display: flex;
  gap: 10px;
}

.result-count {
  color: var(--el-color-primary);
}

.table-scroll {
  overflow-x: auto;
}

.friend-link-table {
  min-width: 1120px;
}

.friend-link-name-cell {
  display: flex;
  gap: 10px;
  align-items: center;

  div {
    display: grid;
    gap: 3px;
    min-width: 0;
  }

  a,
  span {
    overflow: hidden;
    text-overflow: ellipsis;
    font-size: 12px;
    color: var(--el-text-color-secondary);
    white-space: nowrap;
  }
}

.friend-link-pagination {
  justify-content: flex-end;
  margin-top: 18px;
}

@media (width <= 900px) {
  .friend-link-page {
    padding: 12px;
  }

  .filter-grid {
    grid-template-columns: 1fr;
    gap: 0;
  }

  .result-heading,
  .result-actions {
    align-items: flex-start;
    flex-direction: column;
  }

  .friend-link-pagination {
    justify-content: flex-start;
    overflow-x: auto;
  }
}
</style>
