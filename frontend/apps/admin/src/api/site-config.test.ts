import MockAdapter from "axios-mock-adapter";
import { afterEach, describe, expect, it } from "vitest";
import type { SiteConfigPayload } from "@/features/site-config/model";
import { http } from "@/utils/http";
import { getSiteConfig, updateSiteConfig } from "./site-config";

const mock = new MockAdapter(http.instance);

afterEach(() => mock.reset());

const ok = (data: unknown = null) => ({
  code: "00000",
  msg: "success",
  data
});

describe("site config API", () => {
  it("requests admin site config reads and full updates", async () => {
    const payload: SiteConfigPayload = {
      siteTitleZh: "中文标题",
      siteTitleJa: "日本語タイトル",
      siteTitleEn: "English title",
      siteSubtitleZh: "中文副标题",
      siteSubtitleJa: "日本語サブタイトル",
      siteSubtitleEn: "English subtitle",
      aboutMdZh: "中文关于",
      aboutMdJa: "日本語 About",
      aboutMdEn: "English About",
      logoUrl: null,
      faviconUrl: null,
      icpNo: null,
      spotifyPlaylistId: null
    };
    mock.onGet("/api/admin/site-config").reply(200, ok(payload));
    mock.onPut("/api/admin/site-config").reply(config => {
      expect(JSON.parse(config.data)).toEqual(payload);
      return [
        200,
        ok({
          ...payload,
          updatedAt: "2026-06-25T10:00:00",
          updatedBy: "1001"
        })
      ];
    });

    await expect(getSiteConfig()).resolves.toMatchObject({
      data: { siteTitleZh: "中文标题" }
    });
    await expect(updateSiteConfig(payload)).resolves.toMatchObject({
      data: { updatedBy: "1001" }
    });
  });
});
