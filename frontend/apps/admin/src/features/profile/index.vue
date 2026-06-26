<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import AttachmentPickerDialog from "@/features/attachments/AttachmentPickerDialog.vue";
import type { AttachmentItem } from "@/features/attachments/model";
import { transformI18n } from "@/plugins/i18n";
import { useUserStoreHook } from "@/store/modules/user";
import { useProfileManagement } from "./useProfileManagement";

defineOptions({ name: "ProfileManagement" });

const userStore = useUserStoreHook();
const isAdmin = computed(() => userStore.isAdmin);
const state = useProfileManagement();
const {
  currentUser,
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
const avatarPickerOpen = ref(false);

function fieldError(field: keyof typeof form): string {
  const code = formErrors[field];
  return code ? transformI18n(`settings.validation.${code}`) : "";
}

function selectAvatar(item: AttachmentItem): void {
  form.avatarUrl = item.publicUrl;
}

function clearAvatar(): void {
  form.avatarUrl = "";
}

onMounted(initialize);
</script>

<template>
  <section class="profile-page">
    <el-alert
      v-if="readonly"
      data-testid="profile-readonly"
      type="info"
      :closable="false"
      :title="transformI18n('settings.readonlyDemo')"
      show-icon
    />

    <el-skeleton
      v-if="loading && !currentUser"
      data-testid="profile-loading"
      :rows="8"
      animated
    />

    <el-alert
      v-else-if="error"
      data-testid="profile-error"
      type="error"
      :closable="false"
      :title="transformI18n('settings.profile.loadError')"
      show-icon
    >
      <el-button data-testid="profile-retry" type="primary" link @click="refresh">
        {{ transformI18n("articles.actions.retry") }}
      </el-button>
    </el-alert>

    <template v-else>
      <el-card
        data-testid="profile-account-card"
        class="workspace-card"
        shadow="never"
      >
        <template #header>
          <div class="card-heading">
            <h2>{{ transformI18n("settings.profile.account") }}</h2>
          </div>
        </template>
        <div class="account-summary">
          <el-avatar
            :src="currentUser?.profile.avatarUrl || undefined"
            :size="56"
          >
            {{ currentUser?.profile.nickname?.slice(0, 1) || currentUser?.username?.slice(0, 1) }}
          </el-avatar>
          <el-descriptions :column="2" border>
            <el-descriptions-item :label="transformI18n('settings.profile.id')">
              {{ currentUser?.id }}
            </el-descriptions-item>
            <el-descriptions-item :label="transformI18n('settings.profile.username')">
              {{ currentUser?.username }}
            </el-descriptions-item>
            <el-descriptions-item :label="transformI18n('settings.profile.role')">
              <el-tag>{{ currentUser?.type }}</el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </el-card>

      <el-card
        data-testid="profile-form-card"
        class="workspace-card"
        shadow="never"
      >
        <template #header>
          <div class="card-heading">
            <h2>{{ transformI18n("settings.profile.form") }}</h2>
            <el-button
              v-if="isAdmin"
              data-testid="profile-save"
              type="primary"
              :loading="saving"
              @click="save"
            >
              {{ transformI18n("taxonomy.actions.save") }}
            </el-button>
          </div>
        </template>

        <el-alert
          v-if="saveError"
          data-testid="profile-save-error"
          type="error"
          :closable="false"
          :title="transformI18n('settings.profile.saveError')"
          show-icon
        />

        <el-form :model="form" label-position="top" class="profile-grid">
          <el-form-item
            :label="transformI18n('settings.profile.nickname')"
            :error="fieldError('nickname')"
          >
            <el-input v-model="form.nickname" :disabled="readonly" />
          </el-form-item>
          <el-form-item :label="transformI18n('settings.profile.avatarUrl')">
            <div class="image-url-field">
              <el-input
                v-model="form.avatarUrl"
                :disabled="readonly"
                :placeholder="transformI18n('settings.image.currentUrl')"
              />
              <div v-if="form.avatarUrl" class="image-preview image-preview-avatar">
                <el-avatar :src="form.avatarUrl" :size="64">
                  {{ form.nickname?.slice(0, 1) || currentUser?.username?.slice(0, 1) }}
                </el-avatar>
                <span class="image-url">{{ form.avatarUrl }}</span>
              </div>
              <div v-if="isAdmin" class="image-actions">
                <el-button
                  data-testid="profile-avatar-choose"
                  @click="avatarPickerOpen = true"
                >
                  {{ transformI18n("settings.image.choose") }}
                </el-button>
                <el-button
                  v-if="form.avatarUrl"
                  data-testid="profile-avatar-clear"
                  @click="clearAvatar"
                >
                  {{ transformI18n("settings.image.clear") }}
                </el-button>
              </div>
            </div>
          </el-form-item>
          <el-form-item :label="transformI18n('settings.profile.location')">
            <el-input v-model="form.location" :disabled="readonly" />
          </el-form-item>
          <el-form-item :label="transformI18n('settings.profile.website')">
            <el-input v-model="form.website" :disabled="readonly" />
          </el-form-item>
          <el-form-item :label="transformI18n('settings.profile.emailPublic')">
            <el-input v-model="form.emailPublic" :disabled="readonly" />
          </el-form-item>
          <el-form-item :label="transformI18n('settings.profile.githubUrl')">
            <el-input v-model="form.githubUrl" :disabled="readonly" />
          </el-form-item>
          <el-form-item :label="transformI18n('settings.profile.twitterUrl')">
            <el-input v-model="form.twitterUrl" :disabled="readonly" />
          </el-form-item>
          <el-form-item :label="transformI18n('settings.profile.linkedinUrl')">
            <el-input v-model="form.linkedinUrl" :disabled="readonly" />
          </el-form-item>
          <el-form-item :label="transformI18n('settings.profile.zhihuUrl')">
            <el-input v-model="form.zhihuUrl" :disabled="readonly" />
          </el-form-item>
          <el-form-item :label="transformI18n('settings.profile.qiitaUrl')">
            <el-input v-model="form.qiitaUrl" :disabled="readonly" />
          </el-form-item>
          <el-form-item :label="transformI18n('settings.profile.juejinUrl')">
            <el-input v-model="form.juejinUrl" :disabled="readonly" />
          </el-form-item>
          <el-form-item :label="transformI18n('settings.profile.bioZh')">
            <el-input v-model="form.bioZh" type="textarea" :rows="4" :disabled="readonly" />
          </el-form-item>
          <el-form-item :label="transformI18n('settings.profile.bioJa')">
            <el-input v-model="form.bioJa" type="textarea" :rows="4" :disabled="readonly" />
          </el-form-item>
          <el-form-item :label="transformI18n('settings.profile.bioEn')">
            <el-input v-model="form.bioEn" type="textarea" :rows="4" :disabled="readonly" />
          </el-form-item>
        </el-form>
      </el-card>
    </template>

    <AttachmentPickerDialog
      v-model="avatarPickerOpen"
      @select="selectAvatar"
    />
  </section>
</template>

<style scoped lang="scss">
.profile-page {
  display: grid;
  gap: 18px;
  padding: 20px;
  background: var(--el-bg-color-page);
}

.workspace-card {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
}

.card-heading,
.account-summary {
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

.profile-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
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
  .profile-page {
    padding: 12px;
  }

  .account-summary,
  .card-heading {
    align-items: flex-start;
    flex-direction: column;
  }

  .profile-grid {
    grid-template-columns: 1fr;
    gap: 0;
  }
}
</style>
