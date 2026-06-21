<script setup lang="ts">
import { computed } from "vue";
import { transformI18n } from "@/plugins/i18n";
import { useUserStoreHook } from "@/store/modules/user";

defineOptions({ name: "Dashboard" });

const userStore = useUserStoreHook();
const user = computed(() => userStore.currentUser);
const displayName = computed(
  () => user.value?.profile.nickname || user.value?.username || "-"
);
</script>

<template>
  <section class="dashboard-page">
    <el-alert
      v-if="userStore.isDemo"
      data-testid="demo-read-only"
      type="warning"
      :closable="false"
      :title="transformI18n('status.readOnlyDemo')"
      show-icon
    />

    <el-card class="mt-4">
      <template #header>
        {{ transformI18n("dashboard.welcome") }}, {{ displayName }}
      </template>
      <el-descriptions :column="1" border>
        <el-descriptions-item :label="transformI18n('dashboard.account')">
          {{ user?.username }}
        </el-descriptions-item>
        <el-descriptions-item :label="transformI18n('dashboard.role')">
          {{ user?.type }}
        </el-descriptions-item>
        <el-descriptions-item :label="transformI18n('dashboard.backendStatus')">
          <el-tag type="success">
            {{ transformI18n("dashboard.connected") }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
  </section>
</template>

<style scoped>
.dashboard-page {
  padding: 20px;
}
</style>
