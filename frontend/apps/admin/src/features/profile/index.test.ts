import MockAdapter from "axios-mock-adapter";
import { config, flushPromises, mount } from "@vue/test-utils";
import { afterEach, describe, expect, it, vi } from "vitest";
import { sessionService } from "@/features/auth/session";
import { useUserStoreHook } from "@/store/modules/user";
import { http } from "@/utils/http";
import ProfileManagement from "./index.vue";

const { routerReplace, showMessage } = vi.hoisted(() => ({
  routerReplace: vi.fn(),
  showMessage: vi.fn()
}));

vi.mock("vue-router", () => ({ useRouter: () => ({ replace: routerReplace }) }));
vi.mock("@/utils/message", () => ({ message: showMessage }));

const mock = new MockAdapter(http.instance);
config.global.renderStubDefaultSlot = true;

const stubs = {
  "el-alert": true,
  "el-avatar": true,
  "el-button": { template: "<button><slot /></button>" },
  "el-card": { template: "<div><slot name='header' /><slot /></div>" },
  "el-descriptions": true,
  "el-descriptions-item": true,
  "el-dialog": { template: "<div><slot /></div>" },
  "el-empty": true,
  "el-form": true,
  "el-form-item": true,
  "el-image": true,
  "el-input": {
    props: ["modelValue", "type", "disabled"],
    emits: ["update:modelValue"],
    template:
      "<input :type=\"type || 'text'\" :disabled=\"disabled\" :value=\"modelValue\" @input=\"$emit('update:modelValue', $event.target.value)\" />"
  },
  "el-pagination": true,
  "el-skeleton": true,
  "el-tag": true
};

const profile = {
  nickname: "Admin",
  avatarUrl: null,
  bioZh: "中文简介",
  bioJa: null,
  bioEn: "English bio",
  location: null,
  website: "https://example.com",
  emailPublic: null,
  githubUrl: null,
  twitterUrl: null,
  linkedinUrl: null,
  zhihuUrl: null,
  qiitaUrl: null,
  juejinUrl: null
};

const ok = (data: unknown) => ({ code: "00000", msg: "success", data });

const attachmentPage = {
  records: [
    {
      id: "9007199254743001",
      publicUrl: "http://localhost/media/avatar.png",
      contentType: "image/png",
      fileSize: 1024,
      width: 400,
      height: 400,
      originalFilename: "avatar.png",
      createdAt: "2026-06-26T12:00:00",
      createdBy: "1001"
    }
  ],
  total: 1,
  page: 1,
  size: 20
};

function currentUser(type: "ADMIN" | "DEMO") {
  return {
    id: type === "ADMIN" ? "1001" : "1002",
    username: type.toLowerCase(),
    type,
    profile
  };
}

afterEach(() => {
  mock.reset();
  useUserStoreHook().CLEAR_USER();
  routerReplace.mockClear();
  showMessage.mockClear();
  vi.restoreAllMocks();
});

describe("profile management page", () => {
  it("renders editable profile controls for admin users", async () => {
    mock.onGet("/api/auth/me").reply(200, ok(currentUser("ADMIN")));
    const wrapper = mount(ProfileManagement, { global: { stubs } });
    await flushPromises();

    expect(wrapper.find('[data-testid="profile-account-card"]').exists()).toBe(true);
    expect(wrapper.find('[data-testid="profile-form-card"]').exists()).toBe(true);
    expect(wrapper.find('[data-testid="profile-save"]').exists()).toBe(true);
    expect(wrapper.find('[data-testid="profile-password-card"]').exists()).toBe(
      true
    );
    expect(wrapper.find('[data-testid="profile-readonly"]').exists()).toBe(false);
  });

  it("keeps demo users read-only", async () => {
    mock.onGet("/api/auth/me").reply(200, ok(currentUser("DEMO")));
    const wrapper = mount(ProfileManagement, { global: { stubs } });
    await flushPromises();

    expect(wrapper.find('[data-testid="profile-save"]').exists()).toBe(false);
    expect(wrapper.find('[data-testid="profile-password-card"]').exists()).toBe(
      false
    );
    expect(wrapper.find('[data-testid="profile-readonly"]').exists()).toBe(true);
    expect(wrapper.find('[data-testid="profile-avatar-choose"]').exists()).toBe(false);
  });

  it("selects an avatar attachment and saves the public URL", async () => {
    mock.onGet("/api/auth/me").reply(200, ok(currentUser("ADMIN")));
    mock.onGet("/api/admin/attachments").reply(200, ok(attachmentPage));
    mock.onPatch("/api/auth/me/profile").reply(config => {
      expect(JSON.parse(config.data).avatarUrl).toBe(
        "http://localhost/media/avatar.png"
      );
      return [200, ok(profile)];
    });
    const wrapper = mount(ProfileManagement, { global: { stubs } });
    await flushPromises();

    await wrapper.get('[data-testid="profile-avatar-choose"]').trigger("click");
    await flushPromises();
    await wrapper
      .get('[data-testid="attachment-picker-select-9007199254743001"]')
      .trigger("click");
    await wrapper.get('[data-testid="profile-save"]').trigger("click");
    await flushPromises();

    expect(mock.history.patch).toHaveLength(1);
  });

  it("changes an admin password through the existing endpoint and returns to login", async () => {
    mock.onGet("/api/auth/me").reply(200, ok(currentUser("ADMIN")));
    mock.onPut("/api/auth/me/password").reply(config => {
      expect(JSON.parse(config.data)).toEqual({
        currentPassword: "old-password",
        newPassword: "new-password"
      });
      return [200, ok(null)];
    });
    const expire = vi.spyOn(sessionService, "expire");
    const wrapper = mount(ProfileManagement, { global: { stubs } });
    await flushPromises();

    const inputs = wrapper
      .get('[data-testid="profile-password-card"]')
      .findAll("input");
    await inputs[0].setValue("old-password");
    await inputs[1].setValue("new-password");
    await inputs[2].setValue("new-password");
    await wrapper.get('[data-testid="profile-password-save"]').trigger("click");
    await flushPromises();

    expect(mock.history.put).toHaveLength(1);
    expect(expire).toHaveBeenCalledOnce();
    expect(routerReplace).toHaveBeenCalledWith("/login");
    expect(showMessage).toHaveBeenCalledOnce();
  });

  it("keeps password fields after a failed password change", async () => {
    mock.onGet("/api/auth/me").reply(200, ok(currentUser("ADMIN")));
    mock.onPut("/api/auth/me/password").reply(400, ok(null));
    const wrapper = mount(ProfileManagement, { global: { stubs } });
    await flushPromises();

    const card = wrapper.get('[data-testid="profile-password-card"]');
    const inputs = card.findAll("input");
    await inputs[0].setValue("old-password");
    await inputs[1].setValue("new-password");
    await inputs[2].setValue("new-password");
    await card.get('[data-testid="profile-password-save"]').trigger("click");
    await flushPromises();

    expect((inputs[0].element as HTMLInputElement).value).toBe("old-password");
    expect((inputs[1].element as HTMLInputElement).value).toBe("new-password");
    expect((inputs[2].element as HTMLInputElement).value).toBe("new-password");
    expect(wrapper.find('[data-testid="profile-password-error"]').exists()).toBe(
      true
    );
    expect(routerReplace).not.toHaveBeenCalled();
  });
});
