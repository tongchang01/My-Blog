<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { ElMessageBox } from "element-plus";
import AttachmentPickerDialog from "@/features/attachments/AttachmentPickerDialog.vue";
import type { AttachmentItem } from "@/features/attachments/model";
import { transformI18n } from "@/plugins/i18n";
import { useUserStoreHook } from "@/store/modules/user";
import { message } from "@/utils/message";
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
  refresh,
  changePage,
  openCreate,
  openEdit,
  closeDialog,
  save,
  setSortOrder,
  saveSortOrders
} = state;

const avatarPickerOpen = ref(false);

function statusKey(status: FriendLinkStatus): string {
  if (status === "VISIBLE") return "friendLinks.status.visible";
  return "friendLinks.status.hidden";
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

function statusConfirmKey(item: FriendLinkItem): string {
  return item.status === "VISIBLE"
    ? "friendLinks.actions.hideConfirm"
    : "friendLinks.actions.showConfirm";
}

function selectAvatar(item: AttachmentItem): void {
  form.avatarUrl = item.publicUrl;
}

function clearAvatar(): void {
  form.avatarUrl = "";
}

async function confirmStatus(item: FriendLinkItem): Promise<void> {
  try {
    await ElMessageBox.confirm(
      transformI18n(statusConfirmKey(item)),
      transformI18n("friendLinks.actions.confirmTitle"),
      { type: "warning" }
    );
  } catch {
    return;
  }
  if (await state.updateStatus(item.id, nextStatus(item))) {
    message(transformI18n("friendLinks.feedback.statusUpdated"), {
      type: "success"
    });
  }
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
  if (await state.remove(item.id)) {
    message(transformI18n("friendLinks.feedback.deleted"), { type: "success" });
  }
}

async function submitSave(): Promise<void> {
  if (await save()) {
    message(transformI18n("friendLinks.feedback.saved"), { type: "success" });
  }
}

async function submitSort(): Promise<void> {
  if (await saveSortOrders()) {
    message(transformI18n("friendLinks.feedback.sortSaved"), {
      type: "success"
    });
  }
}

onMounted(initialize);
</script>

<template>
  <section class="friend-link-page">
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
              @click="submitSort"
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
      <p v-if="isAdmin" class="field-hint">
        {{ transformI18n("friendLinks.sortHint") }}
      </p>

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
                  @update:model-value="
                    value => setSortOrder(row.id, value ?? row.sortOrder)
                  "
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
      :title="
        transformI18n(
          editingId ? 'friendLinks.dialog.edit' : 'friendLinks.dialog.create'
        )
      "
      width="560px"
      @closed="closeDialog"
    >
      <el-form :model="form" label-position="top">
        <el-form-item
          required
          :label="transformI18n('friendLinks.fields.name')"
          :error="fieldError('name')"
        >
          <el-input v-model="form.name" maxlength="64" />
        </el-form-item>
        <el-form-item
          required
          :label="transformI18n('friendLinks.fields.url')"
          :error="fieldError('url')"
        >
          <el-input v-model="form.url" maxlength="255" />
        </el-form-item>
        <el-form-item
          :label="transformI18n('friendLinks.fields.avatarUrl')"
          :error="fieldError('avatarUrl')"
        >
          <div class="image-url-field">
            <el-input
              v-model="form.avatarUrl"
              maxlength="255"
              :placeholder="transformI18n('settings.image.currentUrl')"
            />
            <div v-if="form.avatarUrl" class="image-preview">
              <el-avatar :src="form.avatarUrl" :size="48">
                {{ form.name.slice(0, 1).toUpperCase() }}
              </el-avatar>
              <span class="image-url">{{ form.avatarUrl }}</span>
            </div>
            <div class="image-actions">
              <el-button
                data-testid="friend-link-avatar-choose"
                @click="avatarPickerOpen = true"
              >
                {{ transformI18n("settings.image.choose") }}
              </el-button>
              <el-button
                v-if="form.avatarUrl"
                data-testid="friend-link-avatar-clear"
                @click="clearAvatar"
              >
                {{ transformI18n("settings.image.clear") }}
              </el-button>
            </div>
          </div>
        </el-form-item>
        <el-form-item :label="transformI18n('friendLinks.fields.description')" :error="fieldError('description')">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="255" show-word-limit />
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
        <el-button
          data-testid="friend-link-save"
          type="primary"
          :loading="saving"
          @click="submitSave"
        >
          {{ transformI18n("taxonomy.actions.save") }}
        </el-button>
      </template>
    </el-dialog>

    <AttachmentPickerDialog v-model="avatarPickerOpen" @select="selectAvatar" />
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

.result-count {
  color: var(--el-color-primary);
}

.field-hint {
  margin: 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
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

.image-url-field {
  display: grid;
  width: 100%;
  gap: 10px;
}

.image-preview {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 10px;
  overflow: hidden;
  background: var(--el-fill-color-lighter);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
}

.image-url {
  min-width: 0;
  overflow: hidden;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.image-actions {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

@media (width <= 900px) {
  .friend-link-page {
    padding: 12px;
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
