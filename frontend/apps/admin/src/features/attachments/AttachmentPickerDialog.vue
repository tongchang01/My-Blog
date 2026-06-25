<script setup lang="ts">
import { watch } from "vue";
import { transformI18n } from "@/plugins/i18n";
import { formatJstDateTime } from "@/features/articles/presentation";
import type { AttachmentItem } from "./model";
import { useAttachmentManagement } from "./useAttachmentManagement";

defineOptions({ name: "AttachmentPickerDialog" });

const props = defineProps<{
  modelValue: boolean;
}>();

const emit = defineEmits<{
  "update:modelValue": [value: boolean];
  select: [item: AttachmentItem];
}>();

const state = useAttachmentManagement();
const {
  pagination,
  items,
  total,
  loading,
  error,
  refresh,
  changePage,
  initialize
} = state;

let initialized = false;

function close(): void {
  emit("update:modelValue", false);
}

function select(item: AttachmentItem): void {
  emit("select", item);
  close();
}

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  const kb = bytes / 1024;
  if (kb < 1024) return `${Number(kb.toFixed(kb >= 10 ? 0 : 1))} KB`;
  const mb = kb / 1024;
  return `${Number(mb.toFixed(mb >= 10 ? 0 : 1))} MB`;
}

watch(
  () => props.modelValue,
  async visible => {
    if (!visible || initialized) return;
    initialized = true;
    await initialize();
  },
  { immediate: true }
);
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    :title="transformI18n('attachments.picker.title')"
    width="760px"
    @update:model-value="value => emit('update:modelValue', value)"
  >
    <div class="picker-toolbar">
      <p>{{ transformI18n("attachments.picker.description") }}</p>
      <el-button data-testid="attachment-picker-refresh" :loading="loading" @click="refresh">
        {{ transformI18n("articles.actions.refresh") }}
      </el-button>
    </div>

    <el-skeleton
      v-if="loading && items.length === 0"
      data-testid="attachment-picker-loading"
      :rows="5"
      animated
    />

    <el-alert
      v-else-if="error"
      data-testid="attachment-picker-error"
      type="error"
      :closable="false"
      :title="transformI18n('attachments.errors.load')"
      show-icon
    />

    <el-empty
      v-else-if="items.length === 0"
      data-testid="attachment-picker-empty"
      :description="transformI18n('attachments.empty')"
    />

    <template v-else>
      <div class="picker-grid">
        <article v-for="item in items" :key="item.id" class="picker-item">
          <el-image
            class="picker-preview"
            :src="item.publicUrl"
            fit="cover"
          />
          <div class="picker-meta">
            <strong>{{ item.originalFilename }}</strong>
            <span>#{{ item.id }}</span>
            <span>{{ item.width }} × {{ item.height }} · {{ formatFileSize(item.fileSize) }}</span>
            <span>{{ formatJstDateTime(item.createdAt) }}</span>
          </div>
          <el-button
            :data-testid="`attachment-picker-select-${item.id}`"
            type="primary"
            @click="select(item)"
          >
            {{ transformI18n("attachments.picker.select") }}
          </el-button>
        </article>
      </div>

      <el-pagination
        class="picker-pagination"
        background
        layout="total, prev, pager, next"
        :current-page="pagination.page"
        :page-size="pagination.size"
        :total="total"
        @current-change="page => changePage(page)"
      />
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.picker-toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;

  p {
    margin: 0;
    color: var(--el-text-color-secondary);
  }
}

.picker-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(190px, 1fr));
  gap: 14px;
}

.picker-item {
  display: grid;
  gap: 10px;
  padding: 10px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
}

.picker-preview {
  width: 100%;
  height: 110px;
  border-radius: 6px;
  background: var(--el-fill-color-light);
}

.picker-meta {
  display: grid;
  gap: 3px;
  min-width: 0;

  span {
    overflow: hidden;
    text-overflow: ellipsis;
    font-size: 12px;
    color: var(--el-text-color-secondary);
    white-space: nowrap;
  }
}

.picker-pagination {
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
