<script setup lang="ts">
import { computed, onMounted } from "vue";
import { ElMessageBox } from "element-plus";
import { transformI18n } from "@/plugins/i18n";
import { useUserStoreHook } from "@/store/modules/user";
import { message } from "@/utils/message";
import { formatJstDateTime } from "@/features/articles/presentation";
import type { AttachmentItem } from "./model";
import { useAttachmentManagement } from "./useAttachmentManagement";

defineOptions({ name: "AttachmentManagement" });

const userStore = useUserStoreHook();
const isAdmin = computed(() => userStore.isAdmin);
const state = useAttachmentManagement();
const {
  pagination,
  items,
  total,
  loading,
  uploading,
  error,
  uploadError,
  operationError,
  showDeleted,
  initialize,
  refresh,
  changePage,
  upload,
  showDeletedAttachments,
  showActiveAttachments,
  remove,
  restore
} = state;

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  const kb = bytes / 1024;
  if (kb < 1024) return `${Number(kb.toFixed(kb >= 10 ? 0 : 1))} KB`;
  const mb = kb / 1024;
  return `${Number(mb.toFixed(mb >= 10 ? 0 : 1))} MB`;
}

function dimensions(item: AttachmentItem): string {
  return `${item.width} × ${item.height}`;
}

async function handleFileChange(event: Event): Promise<void> {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) return;
  if (await upload(file)) {
    message(transformI18n("attachments.feedback.uploaded"), {
      type: "success"
    });
  }
  input.value = "";
}

async function copyUrl(item: AttachmentItem): Promise<void> {
  if (!navigator.clipboard) return;
  await navigator.clipboard.writeText(item.publicUrl);
  message(transformI18n("attachments.feedback.copied"), { type: "success" });
}

async function confirmRemove(item: AttachmentItem): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `${transformI18n("attachments.actions.deleteConfirm")}：${item.originalFilename}`,
      transformI18n("attachments.actions.confirmTitle"),
      { type: "warning" }
    );
  } catch {
    return;
  }
  if (await remove(item.id)) {
    message(transformI18n("attachments.feedback.deleted"), { type: "success" });
  }
}

async function restoreAttachment(id: string): Promise<void> {
  if (await restore(id)) {
    message(transformI18n("attachments.feedback.restored"), { type: "success" });
  }
}

onMounted(initialize);
</script>

<template>
  <section class="attachment-page">
    <el-card
      data-testid="attachment-upload-card"
      class="workspace-card upload-card"
      shadow="never"
    >
      <template #header>
        <div class="card-heading">
          <h2>{{ transformI18n("attachments.upload.title") }}</h2>
        </div>
      </template>

      <div v-if="isAdmin" class="upload-panel">
        <input
          data-testid="attachment-file-input"
          type="file"
          accept="image/jpeg,image/png,image/webp,image/gif"
          :disabled="uploading"
          @change="handleFileChange"
        />
        <p>{{ transformI18n("attachments.upload.description") }}</p>
      </div>
      <el-alert
        v-else
        data-testid="attachment-readonly"
        type="info"
        :closable="false"
        :title="transformI18n('attachments.readonlyDemo')"
        show-icon
      />

      <el-alert
        v-if="uploadError"
        data-testid="attachment-upload-error"
        class="upload-error"
        type="error"
        :closable="false"
        :title="
          transformI18n(
            uploadError.message === 'FILE_TOO_LARGE'
              ? 'attachments.errors.tooLarge'
              : 'attachments.errors.upload'
          )
        "
        show-icon
      />
    </el-card>

    <el-card
      data-testid="attachment-result-card"
      class="workspace-card result-card"
      shadow="never"
    >
      <template #header>
        <div class="card-heading result-heading">
          <h2>
            {{
              transformI18n(
                showDeleted
                  ? "attachments.result.deletedTotal"
                  : "attachments.result.total"
              )
            }}
            <span class="result-count">{{ total }}</span>
          </h2>
          <div class="result-actions">
            <el-button
              v-if="!showDeleted"
              data-testid="attachment-show-deleted"
              @click="showDeletedAttachments"
            >
              {{ transformI18n("attachments.actions.recycleBin") }}
            </el-button>
            <el-button
              v-else
              data-testid="attachment-show-active"
              @click="showActiveAttachments"
            >
              {{ transformI18n("attachments.actions.activeList") }}
            </el-button>
            <el-button
              data-testid="attachment-refresh"
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
        data-testid="attachment-operation-error"
        class="upload-error"
        type="error"
        :closable="false"
        :title="transformI18n('attachments.errors.operation')"
        show-icon
      />

      <el-skeleton
        v-if="loading && items.length === 0"
        data-testid="attachment-loading"
        :rows="6"
        animated
      />

      <el-alert
        v-else-if="error"
        data-testid="attachment-error"
        type="error"
        :closable="false"
        :title="transformI18n('attachments.errors.load')"
        show-icon
      >
        <el-button
          data-testid="attachment-retry"
          type="primary"
          link
          @click="refresh"
        >
          {{ transformI18n("articles.actions.retry") }}
        </el-button>
      </el-alert>

      <el-empty
        v-else-if="items.length === 0"
        data-testid="attachment-empty"
        :description="transformI18n('attachments.empty')"
      />

      <template v-else>
        <div class="attachment-grid">
          <article
            v-for="item in items"
            :key="item.id"
            class="attachment-card"
            data-testid="attachment-card"
          >
            <el-image
              class="attachment-preview"
              :src="item.publicUrl"
              fit="cover"
              :preview-src-list="[item.publicUrl]"
              preview-teleported
            />
            <div class="attachment-body">
              <strong>{{ item.originalFilename }}</strong>
              <span>#{{ item.id }}</span>
              <span
                >{{ dimensions(item) }} ·
                {{ formatFileSize(item.fileSize) }}</span
              >
              <span>{{ item.contentType }}</span>
              <span>{{ formatJstDateTime(item.createdAt) }}</span>
              <a :href="item.publicUrl" target="_blank" rel="noreferrer">
                {{ item.publicUrl }}
              </a>
            </div>
            <div class="attachment-actions">
              <el-button
                :data-testid="`attachment-copy-${item.id}`"
                link
                type="primary"
                @click="copyUrl(item)"
              >
                {{ transformI18n("attachments.actions.copyUrl") }}
              </el-button>
              <el-button
                link
                type="primary"
                tag="a"
                :href="item.publicUrl"
                target="_blank"
              >
                {{ transformI18n("attachments.actions.open") }}
              </el-button>
              <el-button
                v-if="isAdmin && !showDeleted"
                :data-testid="`attachment-delete-${item.id}`"
                link
                type="danger"
                @click="confirmRemove(item)"
              >
                {{ transformI18n("attachments.actions.delete") }}
              </el-button>
              <el-button
                v-if="isAdmin && showDeleted"
                :data-testid="`attachment-restore-${item.id}`"
                link
                type="primary"
                @click="restoreAttachment(item.id)"
              >
                {{ transformI18n("attachments.actions.restore") }}
              </el-button>
            </div>
          </article>
        </div>

        <el-pagination
          class="attachment-pagination"
          background
          layout="total, sizes, prev, pager, next"
          :current-page="pagination.page"
          :page-size="pagination.size"
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
.attachment-page {
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

.upload-panel {
  display: grid;
  gap: 8px;

  p {
    margin: 0;
    color: var(--el-text-color-secondary);
  }
}

.upload-error {
  margin-top: 12px;
}

.result-count {
  color: var(--el-color-primary);
}

.attachment-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}

.attachment-card {
  overflow: hidden;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-bg-color);
}

.attachment-preview {
  display: block;
  width: 100%;
  height: 150px;
  background: var(--el-fill-color-light);
}

.attachment-body {
  display: grid;
  gap: 4px;
  padding: 12px;

  span,
  a {
    overflow: hidden;
    text-overflow: ellipsis;
    font-size: 12px;
    color: var(--el-text-color-secondary);
    white-space: nowrap;
  }
}

.attachment-actions {
  display: flex;
  gap: 8px;
  padding: 0 12px 12px;
}

.attachment-pagination {
  justify-content: flex-end;
  margin-top: 18px;
}

@media (width <= 900px) {
  .attachment-page {
    padding: 12px;
  }

  .result-heading {
    align-items: flex-start;
    flex-direction: column;
  }

  .attachment-pagination {
    justify-content: flex-start;
    overflow-x: auto;
  }
}
</style>
