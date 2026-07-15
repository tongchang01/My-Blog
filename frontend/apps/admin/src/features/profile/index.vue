<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import AttachmentPickerDialog from "@/features/attachments/AttachmentPickerDialog.vue";
import type { AttachmentItem } from "@/features/attachments/model";
import { transformI18n } from "@/plugins/i18n";
import { useUserStoreHook } from "@/store/modules/user";
import { sessionService } from "@/features/auth/session";
import { message } from "@/utils/message";
import { useProfileManagement } from "./useProfileManagement";

defineOptions({ name: "ProfileManagement" });

const userStore = useUserStoreHook();
const router = useRouter();
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
  passwordForm,
  passwordFormErrors,
  passwordSaving,
  passwordError,
  initialize,
  refresh,
  save,
  changePassword
} = state;

const readonly = computed(() => !isAdmin.value);
const avatarPickerOpen = ref(false);

function fieldError(field: keyof typeof form): string {
  const code = formErrors[field];
  return code ? transformI18n(`settings.validation.${code}`) : "";
}

function passwordFieldError(
  field: keyof typeof passwordForm
): string {
  const code = passwordFormErrors[field];
  return code ? transformI18n(`settings.password.validation.${code}`) : "";
}

async function submitPasswordChange(): Promise<void> {
  if (!(await changePassword())) return;
  sessionService.expire();
  message(transformI18n("settings.password.changed"), { type: "success" });
  await router.replace("/login");
}

async function submitProfileSave(): Promise<void> {
  if (await save()) {
    message(transformI18n("settings.profile.saved"), { type: "success" });
  }
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
      <header class="profile-heading">
        <h1>{{ transformI18n("settings.profile.form") }}</h1>
        <el-button
          v-if="isAdmin"
          data-testid="profile-save"
          type="primary"
          :loading="saving"
          @click="submitProfileSave"
        >
          {{ transformI18n("taxonomy.actions.save") }}
        </el-button>
      </header>

      <el-alert
        v-if="saveError"
        data-testid="profile-save-error"
        type="error"
        :closable="false"
        :title="transformI18n('settings.profile.saveError')"
        show-icon
      />

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
          <h2>{{ transformI18n("settings.profile.identity") }}</h2>
        </template>

        <p class="field-hint">
          {{ transformI18n("settings.profile.publicHint") }}
        </p>

        <el-form :model="form" label-position="top" class="profile-grid">
          <el-form-item
            :label="transformI18n('settings.profile.nickname')"
            :error="fieldError('nickname')"
          >
            <el-input v-model="form.nickname" :disabled="readonly" maxlength="64" show-word-limit />
          </el-form-item>
          <el-form-item :label="transformI18n('settings.profile.avatarUrl')" :error="fieldError('avatarUrl')">
            <div class="image-url-field">
              <el-input
                v-model="form.avatarUrl"
                :disabled="readonly"
                maxlength="255"
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
          <el-form-item :label="transformI18n('settings.profile.location')" :error="fieldError('location')">
            <el-input v-model="form.location" :disabled="readonly" maxlength="64" show-word-limit />
          </el-form-item>
          <el-form-item :label="transformI18n('settings.profile.website')" :error="fieldError('website')">
            <el-input v-model="form.website" :disabled="readonly" maxlength="255" />
          </el-form-item>
          <el-form-item :label="transformI18n('settings.profile.emailPublic')" :error="fieldError('emailPublic')">
            <el-input v-model="form.emailPublic" :disabled="readonly" maxlength="128" />
          </el-form-item>
        </el-form>
      </el-card>

      <el-card
        data-testid="profile-social-card"
        class="workspace-card"
        shadow="never"
      >
        <template #header>
          <h2>{{ transformI18n("settings.profile.social") }}</h2>
        </template>
        <el-form :model="form" label-position="top" class="profile-grid">
          <el-form-item :label="transformI18n('settings.profile.githubUrl')" :error="fieldError('githubUrl')">
            <el-input v-model="form.githubUrl" :disabled="readonly" maxlength="255" />
          </el-form-item>
          <el-form-item :label="transformI18n('settings.profile.twitterUrl')" :error="fieldError('twitterUrl')">
            <el-input v-model="form.twitterUrl" :disabled="readonly" maxlength="255" />
          </el-form-item>
          <el-form-item :label="transformI18n('settings.profile.linkedinUrl')" :error="fieldError('linkedinUrl')">
            <el-input v-model="form.linkedinUrl" :disabled="readonly" maxlength="255" />
          </el-form-item>
          <el-form-item :label="transformI18n('settings.profile.zhihuUrl')" :error="fieldError('zhihuUrl')">
            <el-input v-model="form.zhihuUrl" :disabled="readonly" maxlength="255" />
          </el-form-item>
          <el-form-item :label="transformI18n('settings.profile.qiitaUrl')" :error="fieldError('qiitaUrl')">
            <el-input v-model="form.qiitaUrl" :disabled="readonly" maxlength="255" />
          </el-form-item>
          <el-form-item :label="transformI18n('settings.profile.juejinUrl')" :error="fieldError('juejinUrl')">
            <el-input v-model="form.juejinUrl" :disabled="readonly" maxlength="255" />
          </el-form-item>
        </el-form>
      </el-card>

      <el-card
        data-testid="profile-bio-card"
        class="workspace-card"
        shadow="never"
      >
        <template #header>
          <h2>{{ transformI18n("settings.profile.biographies") }}</h2>
        </template>
        <el-form :model="form" label-position="top" class="bio-grid">
          <el-form-item :label="transformI18n('settings.profile.bioZh')" :error="fieldError('bioZh')">
            <el-input v-model="form.bioZh" type="textarea" :rows="4" :disabled="readonly" maxlength="5000" show-word-limit />
          </el-form-item>
          <el-form-item :label="transformI18n('settings.profile.bioJa')" :error="fieldError('bioJa')">
            <el-input v-model="form.bioJa" type="textarea" :rows="4" :disabled="readonly" maxlength="5000" show-word-limit />
          </el-form-item>
          <el-form-item :label="transformI18n('settings.profile.bioEn')" :error="fieldError('bioEn')">
            <el-input v-model="form.bioEn" type="textarea" :rows="4" :disabled="readonly" maxlength="5000" show-word-limit />
          </el-form-item>
        </el-form>
      </el-card>

      <el-card
        v-if="isAdmin"
        data-testid="profile-password-card"
        class="workspace-card"
        shadow="never"
      >
        <template #header>
          <div class="card-heading">
            <h2>{{ transformI18n("settings.password.title") }}</h2>
            <el-button
              data-testid="profile-password-save"
              type="primary"
              :loading="passwordSaving"
              @click="submitPasswordChange"
            >
              {{ transformI18n("settings.password.save") }}
            </el-button>
          </div>
        </template>

        <el-alert
          v-if="passwordError"
          data-testid="profile-password-error"
          type="error"
          :closable="false"
          :title="transformI18n('settings.password.error')"
          show-icon
        />

        <el-form :model="passwordForm" label-position="top" class="password-grid">
          <el-form-item
            :label="transformI18n('settings.password.current')"
            :error="passwordFieldError('currentPassword')"
          >
            <el-input
              v-model="passwordForm.currentPassword"
              type="password"
              show-password
              autocomplete="current-password"
              maxlength="128"
            />
          </el-form-item>
          <el-form-item
            :label="transformI18n('settings.password.new')"
            :error="passwordFieldError('newPassword')"
          >
            <el-input
              v-model="passwordForm.newPassword"
              type="password"
              show-password
              autocomplete="new-password"
              maxlength="128"
            />
          </el-form-item>
          <el-form-item
            :label="transformI18n('settings.password.confirm')"
            :error="passwordFieldError('confirmPassword')"
          >
            <el-input
              v-model="passwordForm.confirmPassword"
              type="password"
              show-password
              autocomplete="new-password"
              maxlength="128"
            />
          </el-form-item>
        </el-form>
        <p class="field-hint">
          {{ transformI18n("settings.password.revokeHint") }}
        </p>
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
  gap: 16px;
  padding: 20px 24px;
  background: var(--el-bg-color-page);
}

.workspace-card {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
}

.profile-heading,
.card-heading,
.account-summary {
  display: flex;
  gap: 16px;
  align-items: center;
  justify-content: space-between;
}

.profile-heading {
  padding-bottom: 20px;
  border-bottom: 1px solid var(--el-border-color-lighter);

  h1 {
    margin: 0;
    font-size: 20px;
    line-height: 28px;
  }
}

.card-heading h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.profile-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px 20px;

  :deep(.el-form-item) {
    margin-bottom: 0;
  }
}

.bio-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;

  :deep(.el-form-item) {
    margin-bottom: 0;
  }
}

.password-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
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

  .password-grid {
    grid-template-columns: 1fr;
    gap: 0;
  }

  .bio-grid {
    grid-template-columns: 1fr;
    gap: 0;
  }
}
</style>
