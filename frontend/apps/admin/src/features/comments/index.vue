<script setup lang="ts">
import { computed, onMounted, watch } from "vue";
import { ElMessageBox } from "element-plus";
import { transformI18n } from "@/plugins/i18n";
import { useUserStoreHook } from "@/store/modules/user";
import { formatJstDateTime } from "@/features/articles/presentation";
import type { CommentAuditStatus, CommentListItem } from "./model";
import { useCommentManagement } from "./useCommentManagement";

defineOptions({ name: "CommentManagement" });

const userStore = useUserStoreHook();
const isAdmin = computed(() => userStore.isAdmin);
const state = useCommentManagement();
const {
  filters,
  items,
  total,
  loading,
  error,
  operationError,
  operatingId,
  replyDialogVisible,
  replyTarget,
  replyContent,
  replySubmitting,
  initialize,
  search,
  reset,
  refresh,
  changePage,
  openReplyDialog,
  closeReplyDialog,
  submitReply
} = state;

const auditStatuses: Array<CommentAuditStatus | "ALL"> = [
  "ALL",
  "PASS",
  "PENDING",
  "HIDDEN"
];
const isGuestbookFilter = computed(
  () => filters.targetType === "GUESTBOOK"
);

function targetTypeKey(targetType: string): string {
  if (targetType === "ARTICLE") return "comments.target.article";
  if (targetType === "GUESTBOOK") return "comments.target.guestbook";
  return "comments.target.all";
}

function auditStatusKey(status: string): string {
  if (status === "PASS") return "comments.audit.pass";
  if (status === "PENDING") return "comments.audit.pending";
  if (status === "HIDDEN") return "comments.audit.hidden";
  return "comments.audit.all";
}

function auditTagType(status: string) {
  if (status === "PASS") return "success";
  if (status === "PENDING") return "warning";
  if (status === "HIDDEN") return "info";
  return "info";
}

function commentSummary(item: CommentListItem): string {
  return item.contentMd.trim() || item.contentHtml.replace(/<[^>]+>/g, "");
}

function targetLabel(item: CommentListItem): string {
  if (item.targetType === "GUESTBOOK") {
    return transformI18n("comments.target.guestbook");
  }
  return `${transformI18n("comments.target.article")} #${item.targetId}`;
}

function canApprove(item: CommentListItem): boolean {
  return !item.deleted && item.auditStatus !== "PASS";
}

function canHide(item: CommentListItem): boolean {
  return !item.deleted && item.auditStatus !== "HIDDEN";
}

function canReply(item: CommentListItem): boolean {
  return isAdmin.value && !item.deleted && item.auditStatus === "PASS";
}

function actionConfirmKey(item: CommentListItem, action: string): string {
  const target = item.targetType === "ARTICLE" ? "Article" : "Guestbook";
  return `comments.actions.${action}${target}Confirm`;
}

function handleReplyDialogBeforeClose(done: () => void): void {
  if (replySubmitting.value) return;
  closeReplyDialog();
  done();
}

async function confirmAction(
  item: CommentListItem,
  titleKey: string,
  action: (id: string) => Promise<boolean>
): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `${transformI18n(titleKey)}：${commentSummary(item)}`,
      transformI18n("comments.actions.confirmTitle"),
      { type: "warning" }
    );
  } catch {
    return;
  }
  await action(item.id);
}

onMounted(initialize);

watch(
  () => filters.targetType,
  targetType => {
    if (targetType === "GUESTBOOK") filters.targetId = "0";
    else if (filters.targetId === "0") filters.targetId = "";
  }
);
</script>

<template>
  <section class="comment-page">
    <el-card
      data-testid="comment-filter-card"
      class="workspace-card filter-card"
      shadow="never"
    >
      <template #header>
        <div class="card-heading">
          <h2>{{ transformI18n("comments.filter.title") }}</h2>
        </div>
      </template>

      <el-form :model="filters" label-position="top" class="filter-grid">
        <el-form-item :label="transformI18n('comments.filter.targetType')">
          <el-select
            v-model="filters.targetType"
            data-testid="comment-target-type"
          >
            <el-option
              v-for="type in ['ALL', 'ARTICLE', 'GUESTBOOK']"
              :key="type"
              :label="transformI18n(targetTypeKey(type))"
              :value="type"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          v-if="!isGuestbookFilter"
          :label="transformI18n('comments.filter.targetId')"
        >
          <el-input
            v-model="filters.targetId"
            data-testid="comment-target-id"
            clearable
            :placeholder="transformI18n('comments.filter.targetIdPlaceholder')"
            @keyup.enter="search"
          />
        </el-form-item>
        <el-form-item :label="transformI18n('comments.filter.auditStatus')">
          <el-select
            v-model="filters.auditStatus"
            data-testid="comment-audit-status"
          >
            <el-option
              v-for="status in auditStatuses"
              :key="status"
              :label="transformI18n(auditStatusKey(status))"
              :value="status"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="transformI18n('comments.filter.keyword')">
          <el-input
            v-model="filters.keyword"
            data-testid="comment-keyword"
            clearable
            :placeholder="transformI18n('comments.filter.keywordPlaceholder')"
            @keyup.enter="search"
          />
        </el-form-item>
      </el-form>

      <div class="filter-actions">
        <el-checkbox
          v-model="filters.includeDeleted"
          data-testid="comment-include-deleted"
        >
          {{ transformI18n("comments.filter.includeDeleted") }}
        </el-checkbox>
        <div class="filter-buttons">
          <el-button data-testid="comment-search" type="primary" @click="search">
            {{ transformI18n("articles.actions.search") }}
          </el-button>
          <el-button data-testid="comment-reset" @click="reset">
            {{ transformI18n("articles.actions.reset") }}
          </el-button>
        </div>
      </div>
    </el-card>

    <el-card
      data-testid="comment-result-card"
      class="workspace-card result-card"
      shadow="never"
    >
      <template #header>
        <div class="card-heading result-heading">
          <h2>
            {{ transformI18n("comments.result.total") }}
            <span class="result-count">{{ total }}</span>
          </h2>
          <el-button data-testid="comment-refresh" :loading="loading" @click="refresh">
            {{ transformI18n("articles.actions.refresh") }}
          </el-button>
        </div>
      </template>

      <el-alert
        v-if="operationError"
        data-testid="comment-operation-error"
        type="error"
        :closable="false"
        :title="transformI18n('comments.errors.operation')"
        show-icon
      />

      <el-skeleton
        v-if="loading && items.length === 0"
        data-testid="comment-loading"
        :rows="7"
        animated
      />

      <el-alert
        v-else-if="error"
        data-testid="comment-error"
        type="error"
        :closable="false"
        :title="transformI18n('comments.errors.load')"
        show-icon
      >
        <el-button
          data-testid="comment-retry"
          type="primary"
          link
          @click="refresh"
        >
          {{ transformI18n("articles.actions.retry") }}
        </el-button>
      </el-alert>

      <el-empty
        v-else-if="items.length === 0"
        data-testid="comment-empty"
        :description="transformI18n('comments.empty')"
      />

      <template v-else>
        <div class="table-scroll">
          <el-table :data="items" row-key="id" class="comment-table">
            <el-table-column
              :label="transformI18n('comments.columns.content')"
              min-width="260"
            >
              <template #default="{ row }">
                <div class="comment-content-cell">
                  <strong>{{ commentSummary(row) }}</strong>
                  <span v-if="row.replyToNickname" class="reply-meta">
                    {{ transformI18n("comments.reply.replyTo") }}
                    @{{ row.replyToNickname }}
                  </span>
                  <span>#{{ row.id }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column
              :label="transformI18n('comments.columns.author')"
              min-width="190"
            >
              <template #default="{ row }">
                <div class="comment-author-cell">
                  <strong>{{ row.authorNickname }}</strong>
                  <span>{{ row.authorEmail || "—" }}</span>
                  <details v-if="isAdmin" class="comment-audit-details">
                    <summary>{{ transformI18n("comments.auditDetails.title") }}</summary>
                    <span>{{ transformI18n("comments.auditDetails.site") }}：{{ row.authorSite || "—" }}</span>
                    <span>{{ transformI18n("comments.auditDetails.ip") }}：{{ row.authorIp || "—" }}</span>
                    <span>{{ transformI18n("comments.auditDetails.userAgent") }}：{{ row.authorUserAgent || "—" }}</span>
                    <span>{{ transformI18n("comments.auditDetails.parent") }}：{{ row.parentId || "—" }}</span>
                  </details>
                </div>
              </template>
            </el-table-column>
            <el-table-column
              :label="transformI18n('comments.columns.target')"
              min-width="190"
            >
              <template #default="{ row }">
                {{ targetLabel(row) }}
              </template>
            </el-table-column>
            <el-table-column
              :label="transformI18n('comments.columns.auditStatus')"
              width="110"
            >
              <template #default="{ row }">
                <el-tag :type="auditTagType(row.auditStatus)" effect="light">
                  {{ transformI18n(auditStatusKey(row.auditStatus)) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column
              :label="transformI18n('comments.columns.deleted')"
              width="100"
            >
              <template #default="{ row }">
                <el-tag :type="row.deleted ? 'danger' : 'success'" effect="plain">
                  {{
                    transformI18n(
                      row.deleted ? "comments.deleted.yes" : "comments.deleted.no"
                    )
                  }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column
              :label="transformI18n('comments.columns.createdAt')"
              width="155"
            >
              <template #default="{ row }">
                {{ formatJstDateTime(row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column
              v-if="isAdmin"
              data-testid="comment-operation-column"
              :label="transformI18n('articles.columns.operations')"
              fixed="right"
              width="280"
            >
              <template #default="{ row }">
                <el-button
                  v-if="canReply(row)"
                  :data-testid="`comment-reply-${row.id}`"
                  link
                  type="primary"
                  :disabled="operatingId !== null"
                  @click="openReplyDialog(row)"
                >
                  {{ transformI18n("comments.actions.reply") }}
                </el-button>
                <el-button
                  v-if="canApprove(row)"
                  :data-testid="`comment-approve-${row.id}`"
                  link
                  type="success"
                  :loading="operatingId === row.id"
                  :disabled="operatingId !== null"
                  @click="
                    confirmAction(row, actionConfirmKey(row, 'approve'), state.approve)
                  "
                >
                  {{ transformI18n("comments.actions.approve") }}
                </el-button>
                <el-button
                  v-if="canHide(row)"
                  :data-testid="`comment-hide-${row.id}`"
                  link
                  type="warning"
                  :loading="operatingId === row.id"
                  :disabled="operatingId !== null"
                  @click="confirmAction(row, actionConfirmKey(row, 'hide'), state.hide)"
                >
                  {{ transformI18n("comments.actions.hide") }}
                </el-button>
                <el-button
                  v-if="!row.deleted"
                  :data-testid="`comment-delete-${row.id}`"
                  link
                  type="danger"
                  :loading="operatingId === row.id"
                  :disabled="operatingId !== null"
                  @click="
                    confirmAction(row, actionConfirmKey(row, 'delete'), state.remove)
                  "
                >
                  {{ transformI18n("articles.actions.delete") }}
                </el-button>
                <el-button
                  v-if="row.deleted"
                  :data-testid="`comment-restore-${row.id}`"
                  link
                  type="primary"
                  :loading="operatingId === row.id"
                  :disabled="operatingId !== null"
                  @click="
                    confirmAction(row, actionConfirmKey(row, 'restore'), state.restore)
                  "
                >
                  {{ transformI18n("articles.recycle.restore") }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <el-pagination
          class="comment-pagination"
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
      v-model="replyDialogVisible"
      data-testid="comment-reply-dialog"
      :title="transformI18n('comments.reply.title')"
      width="520px"
      :before-close="handleReplyDialogBeforeClose"
    >
      <p v-if="replyTarget" class="reply-target">
        {{ transformI18n("comments.reply.target") }}：{{
          replyTarget.authorNickname
        }}
      </p>
      <el-input
        v-model="replyContent"
        data-testid="comment-reply-content"
        type="textarea"
        :rows="5"
        :placeholder="transformI18n('comments.reply.placeholder')"
      />
      <template #footer>
        <el-button @click="closeReplyDialog">
          {{ transformI18n("articles.actions.cancel") }}
        </el-button>
        <el-button
          data-testid="comment-reply-submit"
          type="primary"
          :loading="replySubmitting"
          :disabled="!replyContent.trim()"
          @click="submitReply"
        >
          {{ transformI18n("comments.reply.submit") }}
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped lang="scss">
.comment-page {
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
.result-heading {
  display: flex;
  gap: 16px;
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
  grid-template-columns: repeat(4, minmax(0, 1fr));
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

.comment-table {
  min-width: 1180px;
}

.comment-content-cell,
.comment-author-cell {
  display: grid;
  gap: 3px;

  span {
    overflow: hidden;
    text-overflow: ellipsis;
    font-size: 12px;
    color: var(--el-text-color-secondary);
    white-space: nowrap;
  }
}

.reply-meta {
  color: var(--el-color-primary);
}

.comment-audit-details {
  display: grid;
  gap: 3px;
  margin-top: 4px;

  summary {
    cursor: pointer;
    font-size: 12px;
    color: var(--el-color-primary);
  }
}

.reply-target {
  margin: 0 0 12px;
  color: var(--el-text-color-secondary);
}

.comment-pagination {
  justify-content: flex-end;
  margin-top: 18px;
}

@media (width <= 900px) {
  .comment-page {
    padding: 12px;
  }

  .filter-grid {
    grid-template-columns: 1fr;
    gap: 0;
  }

  .filter-actions {
    align-items: flex-start;
    flex-direction: column;
  }

  .comment-pagination {
    justify-content: flex-start;
    overflow-x: auto;
  }
}
</style>
