<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import AttachmentPickerDialog from "@/features/attachments/AttachmentPickerDialog.vue";
import type { AttachmentItem } from "@/features/attachments/model";
import { transformI18n } from "@/plugins/i18n";
import { useUserStoreHook } from "@/store/modules/user";
import { message } from "@/utils/message";
import { formatJstDateTime } from "@/features/articles/presentation";
import { useSiteConfigManagement } from "./useSiteConfigManagement";

defineOptions({ name: "SiteConfigManagement" });

type ImageField = "logoUrl" | "faviconUrl";

const userStore = useUserStoreHook();
const isAdmin = computed(() => userStore.isAdmin);
const state = useSiteConfigManagement();
const {
  current,
  form,
  formErrors,
  loading,
  saving,
  error,
  saveError,
  initialize,
  refresh,
  save
} = state;

const readonly = computed(() => !isAdmin.value);
const imagePickerOpen = ref(false);
const imagePickerTarget = ref<ImageField | null>(null);

function fieldError(field: keyof typeof form): string {
  const code = formErrors[field];
  return code ? transformI18n(`settings.validation.${code}`) : "";
}

function openImagePicker(field: ImageField): void {
  imagePickerTarget.value = field;
  imagePickerOpen.value = true;
}

function selectImage(item: AttachmentItem): void {
  if (!imagePickerTarget.value) return;
  form[imagePickerTarget.value] = item.publicUrl;
}

function clearImage(field: ImageField): void {
  form[field] = "";
}

async function submitSave(): Promise<void> {
  if (await save()) {
    message(transformI18n("settings.siteConfig.saved"), { type: "success" });
  }
}

onMounted(initialize);
</script>

<template>
  <section class="settings-page">
    <el-alert
      v-if="readonly"
      data-testid="site-config-readonly"
      type="info"
      :closable="false"
      :title="transformI18n('settings.readonlyDemo')"
      show-icon
    />

    <el-skeleton
      v-if="loading && !current"
      data-testid="site-config-loading"
      :rows="8"
      animated
    />

    <el-alert
      v-else-if="error"
      data-testid="site-config-error"
      type="error"
      :closable="false"
      :title="transformI18n('settings.siteConfig.loadError')"
      show-icon
    >
      <el-button data-testid="site-config-retry" type="primary" link @click="refresh">
        {{ transformI18n("articles.actions.retry") }}
      </el-button>
    </el-alert>

    <template v-else>
      <el-card
        data-testid="site-config-basic-card"
        class="workspace-card"
        shadow="never"
      >
        <template #header>
          <div class="card-heading">
            <div>
              <h2>{{ transformI18n("settings.siteConfig.basic") }}</h2>
              <p v-if="current">
                {{ transformI18n("settings.updatedAt") }}
                {{ formatJstDateTime(current.updatedAt) }}
              </p>
            </div>
            <el-button
              v-if="isAdmin"
              data-testid="site-config-save"
              type="primary"
              :loading="saving"
              @click="submitSave"
            >
              {{ transformI18n("taxonomy.actions.save") }}
            </el-button>
          </div>
        </template>

        <el-alert
          v-if="saveError"
          data-testid="site-config-save-error"
          type="error"
          :closable="false"
          :title="transformI18n('settings.siteConfig.saveError')"
          show-icon
        />

        <p class="field-hint">
          {{ transformI18n("settings.siteConfig.fullReplaceHint") }}
        </p>

        <el-form :model="form" label-position="top" class="settings-grid">
          <el-form-item
            :label="transformI18n('settings.siteConfig.siteTitleZh')"
            :error="fieldError('siteTitleZh')"
          >
          <el-input v-model="form.siteTitleZh" :disabled="readonly" maxlength="128" show-word-limit />
          </el-form-item>
          <el-form-item
            :label="transformI18n('settings.siteConfig.siteTitleJa')"
            :error="fieldError('siteTitleJa')"
          >
            <el-input v-model="form.siteTitleJa" :disabled="readonly" maxlength="128" show-word-limit />
            <p class="field-hint">
              {{ transformI18n("settings.siteConfig.fallbackHint") }}
            </p>
          </el-form-item>
          <el-form-item
            :label="transformI18n('settings.siteConfig.siteTitleEn')"
            :error="fieldError('siteTitleEn')"
          >
            <el-input v-model="form.siteTitleEn" :disabled="readonly" maxlength="128" show-word-limit />
            <p class="field-hint">
              {{ transformI18n("settings.siteConfig.fallbackHint") }}
            </p>
          </el-form-item>
          <el-form-item :label="transformI18n('settings.siteConfig.siteSubtitleZh')" :error="fieldError('siteSubtitleZh')">
            <el-input v-model="form.siteSubtitleZh" :disabled="readonly" maxlength="255" show-word-limit />
          </el-form-item>
          <el-form-item :label="transformI18n('settings.siteConfig.siteSubtitleJa')" :error="fieldError('siteSubtitleJa')">
            <el-input v-model="form.siteSubtitleJa" :disabled="readonly" maxlength="255" show-word-limit />
          </el-form-item>
          <el-form-item :label="transformI18n('settings.siteConfig.siteSubtitleEn')" :error="fieldError('siteSubtitleEn')">
            <el-input v-model="form.siteSubtitleEn" :disabled="readonly" maxlength="255" show-word-limit />
          </el-form-item>
          <el-form-item :label="transformI18n('settings.siteConfig.logoUrl')" :error="fieldError('logoUrl')">
            <div class="image-url-field">
              <el-input
                v-model="form.logoUrl"
                :disabled="readonly"
                maxlength="255"
                :placeholder="transformI18n('settings.image.currentUrl')"
              />
              <div v-if="form.logoUrl" class="image-preview">
                <el-image
                  class="site-image-preview"
                  :src="form.logoUrl"
                  fit="contain"
                />
                <span class="image-url">{{ form.logoUrl }}</span>
              </div>
              <div v-if="isAdmin" class="image-actions">
                <el-button
                  data-testid="site-config-logo-choose"
                  @click="openImagePicker('logoUrl')"
                >
                  {{ transformI18n("settings.image.choose") }}
                </el-button>
                <el-button
                  v-if="form.logoUrl"
                  data-testid="site-config-logo-clear"
                  @click="clearImage('logoUrl')"
                >
                  {{ transformI18n("settings.image.clear") }}
                </el-button>
              </div>
            </div>
          </el-form-item>
          <el-form-item :label="transformI18n('settings.siteConfig.faviconUrl')" :error="fieldError('faviconUrl')">
            <div class="image-url-field">
              <el-input
                v-model="form.faviconUrl"
                :disabled="readonly"
                maxlength="255"
                :placeholder="transformI18n('settings.image.currentUrl')"
              />
              <div v-if="form.faviconUrl" class="image-preview">
                <el-image
                  class="favicon-image-preview"
                  :src="form.faviconUrl"
                  fit="contain"
                />
                <span class="image-url">{{ form.faviconUrl }}</span>
              </div>
              <div v-if="isAdmin" class="image-actions">
                <el-button
                  data-testid="site-config-favicon-choose"
                  @click="openImagePicker('faviconUrl')"
                >
                  {{ transformI18n("settings.image.choose") }}
                </el-button>
                <el-button
                  v-if="form.faviconUrl"
                  data-testid="site-config-favicon-clear"
                  @click="clearImage('faviconUrl')"
                >
                  {{ transformI18n("settings.image.clear") }}
                </el-button>
              </div>
            </div>
          </el-form-item>
          <el-form-item :label="transformI18n('settings.siteConfig.icpNo')" :error="fieldError('icpNo')">
            <el-input v-model="form.icpNo" :disabled="readonly" maxlength="64" show-word-limit />
          </el-form-item>
          <el-form-item :label="transformI18n('settings.siteConfig.spotifyPlaylistId')" :error="fieldError('spotifyPlaylistId')">
            <el-input v-model="form.spotifyPlaylistId" :disabled="readonly" maxlength="64" show-word-limit />
          </el-form-item>
          <el-form-item :label="transformI18n('settings.siteConfig.startedDate')">
            <el-date-picker
              v-model="form.startedDate"
              type="date"
              value-format="YYYY-MM-DD"
              :disabled="readonly"
            />
          </el-form-item>
        </el-form>
      </el-card>

      <el-card
        data-testid="site-config-about-card"
        class="workspace-card"
        shadow="never"
      >
        <template #header>
          <h2>{{ transformI18n("settings.siteConfig.about") }}</h2>
        </template>
        <el-form :model="form" label-position="top">
          <el-form-item :label="transformI18n('settings.siteConfig.aboutMdZh')" :error="fieldError('aboutMdZh')">
            <el-input v-model="form.aboutMdZh" type="textarea" :rows="5" :disabled="readonly" maxlength="50000" show-word-limit />
          </el-form-item>
          <el-form-item :label="transformI18n('settings.siteConfig.aboutMdJa')" :error="fieldError('aboutMdJa')">
            <el-input v-model="form.aboutMdJa" type="textarea" :rows="5" :disabled="readonly" maxlength="50000" show-word-limit />
          </el-form-item>
          <el-form-item :label="transformI18n('settings.siteConfig.aboutMdEn')" :error="fieldError('aboutMdEn')">
            <el-input v-model="form.aboutMdEn" type="textarea" :rows="5" :disabled="readonly" maxlength="50000" show-word-limit />
          </el-form-item>
        </el-form>
      </el-card>
    </template>

    <AttachmentPickerDialog
      v-model="imagePickerOpen"
      @select="selectImage"
    />
  </section>
</template>

<style scoped lang="scss">
.settings-page {
  display: grid;
  gap: 18px;
  padding: 20px;
  background: var(--el-bg-color-page);
}

.workspace-card {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
}

.card-heading {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  justify-content: space-between;

  h2,
  p {
    margin: 0;
  }

  p {
    margin-top: 4px;
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }
}

.settings-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.field-hint {
  margin: 6px 0 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
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

.site-image-preview {
  width: 96px;
  height: 48px;
  background: var(--el-bg-color);
  border-radius: 6px;
}

.favicon-image-preview {
  width: 48px;
  height: 48px;
  background: var(--el-bg-color);
  border-radius: 6px;
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
  .settings-page {
    padding: 12px;
  }

  .settings-grid {
    grid-template-columns: 1fr;
    gap: 0;
  }
}
</style>
