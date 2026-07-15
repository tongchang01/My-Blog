import { describe, expect, it } from "vitest";
import type { SiteConfig } from "./model";
import {
  createSiteConfigForm,
  siteConfigFormToPayload,
  siteConfigToForm,
  validateSiteConfigForm
} from "./form";

const config: SiteConfig = {
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
  faviconUrl: "https://example.com/favicon.ico",
  icpNo: null,
  spotifyPlaylistId: "playlist-id",
  startedDate: "2024-01-02",
  updatedAt: "2026-06-25T10:00:00",
  updatedBy: "1001"
};

describe("site config form", () => {
  it("creates defaults and maps nullable detail fields to editable strings", () => {
    expect(createSiteConfigForm()).toEqual({
      siteTitleZh: "",
      siteTitleJa: "",
      siteTitleEn: "",
      siteSubtitleZh: "",
      siteSubtitleJa: "",
      siteSubtitleEn: "",
      aboutMdZh: "",
      aboutMdJa: "",
      aboutMdEn: "",
      logoUrl: "",
      faviconUrl: "",
      icpNo: "",
      spotifyPlaylistId: "",
      startedDate: ""
    });
    expect(siteConfigToForm(config)).toMatchObject({
      siteTitleZh: "中文标题",
      logoUrl: "",
      faviconUrl: "https://example.com/favicon.ico",
      icpNo: "",
      startedDate: "2024-01-02"
    });
  });

  it("requires only the Chinese title", () => {
    expect(validateSiteConfigForm(createSiteConfigForm())).toEqual({
      siteTitleZh: "required"
    });
  });

  it("validates backend field limits, URLs and Spotify IDs before a full PUT", () => {
    expect(
      validateSiteConfigForm({
        ...siteConfigToForm(config),
        siteTitleZh: "x".repeat(129),
        logoUrl: "ftp://example.com/logo.png",
        spotifyPlaylistId: "invalid id"
      })
    ).toEqual({
      siteTitleZh: "maxLength",
      logoUrl: "url",
      spotifyPlaylistId: "spotifyId"
    });
  });

  it("normalizes whitespace and emits a complete PUT payload", () => {
    expect(
      siteConfigFormToPayload({
        ...siteConfigToForm(config),
        siteTitleZh: " 中文标题 ",
        logoUrl: " ",
        icpNo: " ICP  "
      })
    ).toEqual({
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
      faviconUrl: "https://example.com/favicon.ico",
      icpNo: "ICP",
      spotifyPlaylistId: "playlist-id",
      startedDate: "2024-01-02"
    });
  });
});
