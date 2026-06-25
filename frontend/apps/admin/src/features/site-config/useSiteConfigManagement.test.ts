import { describe, expect, it, vi } from "vitest";
import type { ApiResponse } from "@/api/contract";
import type { SiteConfig } from "./model";
import {
  type SiteConfigManagementApi,
  useSiteConfigManagement
} from "./useSiteConfigManagement";

function ok<T>(data: T): ApiResponse<T> {
  return { code: "00000", msg: "success", data };
}

const config: SiteConfig = {
  siteTitleZh: "中文标题",
  siteTitleJa: "日本語タイトル",
  siteTitleEn: "English title",
  siteSubtitleZh: "中文副标题",
  siteSubtitleJa: null,
  siteSubtitleEn: null,
  aboutMdZh: "中文关于",
  aboutMdJa: null,
  aboutMdEn: null,
  logoUrl: null,
  faviconUrl: null,
  icpNo: null,
  spotifyPlaylistId: null,
  updatedAt: "2026-06-25T10:00:00",
  updatedBy: "1001"
};

function api(
  overrides: Partial<SiteConfigManagementApi> = {}
): SiteConfigManagementApi {
  return {
    getSiteConfig: vi.fn().mockResolvedValue(ok(config)),
    updateSiteConfig: vi.fn().mockResolvedValue(ok(config)),
    ...overrides
  };
}

describe("site config management state", () => {
  it("loads config into the editable form", async () => {
    const source = api();
    const state = useSiteConfigManagement(source);

    await state.initialize();

    expect(source.getSiteConfig).toHaveBeenCalledOnce();
    expect(state.current.value?.siteTitleZh).toBe("中文标题");
    expect(state.form.siteTitleJa).toBe("日本語タイトル");
    expect(state.loading.value).toBe(false);
    expect(state.error.value).toBeNull();
  });

  it("saves a complete payload and refreshes metadata from the response", async () => {
    const source = api();
    const state = useSiteConfigManagement(source);
    await state.initialize();
    state.form.siteTitleZh = " 新标题 ";

    await expect(state.save()).resolves.toBe(true);

    expect(source.updateSiteConfig).toHaveBeenCalledWith(
      expect.objectContaining({ siteTitleZh: "新标题", logoUrl: null })
    );
    expect(state.current.value?.updatedBy).toBe("1001");
    expect(state.saveError.value).toBeNull();
  });

  it("keeps form data and exposes save errors", async () => {
    const source = api({
      updateSiteConfig: vi.fn().mockRejectedValue(new Error("offline"))
    });
    const state = useSiteConfigManagement(source);
    await state.initialize();
    state.form.siteTitleZh = "离线标题";

    await expect(state.save()).resolves.toBe(false);

    expect(state.form.siteTitleZh).toBe("离线标题");
    expect(state.saveError.value?.message).toBe("offline");
  });
});
