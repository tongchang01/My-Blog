import { mount } from "@vue/test-utils";
import { afterEach, describe, expect, it } from "vitest";
import { localesConfigs } from "@/plugins/i18n";
import { useUserStoreHook } from "@/store/modules/user";
import Dashboard from "./index.vue";

const userStore = useUserStoreHook();

afterEach(() => userStore.CLEAR_USER());

describe("admin dashboard", () => {
  it("shows the localized read-only marker for a demo account", () => {
    userStore.SET_CURRENT_USER({
      id: "1",
      username: "demo",
      type: "DEMO",
      profile: {
        nickname: "Demo",
        avatarUrl: null,
        bioZh: null,
        bioJa: null,
        bioEn: null,
        location: null,
        website: null,
        emailPublic: null,
        githubUrl: null,
        twitterUrl: null,
        linkedinUrl: null,
        zhihuUrl: null,
        qiitaUrl: null,
        juejinUrl: null
      }
    });

    const wrapper = mount(Dashboard, {
      global: {
        stubs: {
          "el-alert": true,
          "el-card": true,
          "el-descriptions": true,
          "el-descriptions-item": true,
          "el-tag": true
        }
      }
    });

    expect(
      wrapper.get('[data-testid="demo-read-only"]').attributes("title")
    ).not.toBe("");
    expect(localesConfigs.zh.status.readOnlyDemo).toBeTruthy();
    expect(localesConfigs.ja.status.readOnlyDemo).toBeTruthy();
    expect(localesConfigs.en.status.readOnlyDemo).toBeTruthy();
    expect(wrapper.find("[data-statistic]").exists()).toBe(false);
  });
});
