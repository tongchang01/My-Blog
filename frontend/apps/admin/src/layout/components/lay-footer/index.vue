<script setup lang="ts">
import { onMounted, ref } from "vue";
import { getConfig } from "@/config";
import { getSiteConfig } from "@/api/site-config";

const TITLE = getConfig("Title");
const icpNo = ref("");

onMounted(async () => {
  try {
    icpNo.value = (await getSiteConfig()).data.icpNo?.trim() ?? "";
  } catch {
    // 备案信息加载失败时不影响后台使用。
  }
});
</script>

<template>
  <footer
    class="layout-footer text-[rgba(0,0,0,0.6)] dark:text-[rgba(220,220,242,0.8)]"
  >
    <span>Copyright © 2020-present {{ TITLE }}</span>
    <a
      v-if="icpNo"
      data-testid="footer-icp"
      class="hover:text-primary!"
      href="https://beian.miit.gov.cn/"
      target="_blank"
      rel="noreferrer"
    >
      {{ icpNo }}
    </a>
  </footer>
</template>

<style lang="scss" scoped>
.layout-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  width: 100%;
  padding: 0 0 8px;
  font-size: 14px;
}
</style>
