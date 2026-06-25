<script setup lang="ts">
import { computed, onMounted } from "vue";
import { transformI18n } from "@/plugins/i18n";
import { useUserStoreHook } from "@/store/modules/user";
import { formatJstDateTime } from "@/features/articles/presentation";
import { useSiteConfigManagement } from "./useSiteConfigManagement";

defineOptions({ name: "SiteConfigManagement" });

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

function fieldError(field: keyof typeof form): string {
  const code = formErrors[field];
  return code ? transformI18n(`settings.validation.${code}`) : "";
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
              @click="save"
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

        <el-form :model="form" label-position="top" class="settings-grid">
          <el-form-item
            :label="transformI18n('settings.siteConfig.siteTitleZh')"
            :error="fieldError('siteTitleZh')"
          >
            <el-input v-model="form.siteTitleZh" :disabled="readonly" />
          </el-form-item>
          <el-form-item
            :label="transformI18n('settings.siteConfig.siteTitleJa')"
            :error="fieldError('siteTitleJa')"
          >
            <el-input v-model="form.siteTitleJa" :disabled="readonly" />
          </el-form-item>
          <el-form-item
            :label="transformI18n('settings.siteConfig.siteTitleEn')"
            :error="fieldError('siteTitleEn')"
          >
            <el-input v-model="form.siteTitleEn" :disabled="readonly" />
          </el-form-item>
          <el-form-item :label="transformI18n('settings.siteConfig.siteSubtitleZh')">
            <el-input v-model="form.siteSubtitleZh" :disabled="readonly" />
          </el-form-item>
          <el-form-item :label="transformI18n('settings.siteConfig.siteSubtitleJa')">
            <el-input v-model="form.siteSubtitleJa" :disabled="readonly" />
          </el-form-item>
          <el-form-item :label="transformI18n('settings.siteConfig.siteSubtitleEn')">
            <el-input v-model="form.siteSubtitleEn" :disabled="readonly" />
          </el-form-item>
          <el-form-item :label="transformI18n('settings.siteConfig.logoUrl')">
            <el-input v-model="form.logoUrl" :disabled="readonly" />
          </el-form-item>
          <el-form-item :label="transformI18n('settings.siteConfig.faviconUrl')">
            <el-input v-model="form.faviconUrl" :disabled="readonly" />
          </el-form-item>
          <el-form-item :label="transformI18n('settings.siteConfig.icpNo')">
            <el-input v-model="form.icpNo" :disabled="readonly" />
          </el-form-item>
          <el-form-item :label="transformI18n('settings.siteConfig.spotifyPlaylistId')">
            <el-input v-model="form.spotifyPlaylistId" :disabled="readonly" />
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
          <el-form-item :label="transformI18n('settings.siteConfig.aboutMdZh')">
            <el-input v-model="form.aboutMdZh" type="textarea" :rows="5" :disabled="readonly" />
          </el-form-item>
          <el-form-item :label="transformI18n('settings.siteConfig.aboutMdJa')">
            <el-input v-model="form.aboutMdJa" type="textarea" :rows="5" :disabled="readonly" />
          </el-form-item>
          <el-form-item :label="transformI18n('settings.siteConfig.aboutMdEn')">
            <el-input v-model="form.aboutMdEn" type="textarea" :rows="5" :disabled="readonly" />
          </el-form-item>
        </el-form>
      </el-card>
    </template>
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
