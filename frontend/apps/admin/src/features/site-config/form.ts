import type { SiteConfig, SiteConfigPayload } from "./model";

export type SiteConfigForm = Record<keyof SiteConfigPayload, string>;

export type SiteConfigFormErrorCode = "required";

export type SiteConfigFormErrors = Partial<
  Record<keyof SiteConfigForm, SiteConfigFormErrorCode>
>;

const SITE_CONFIG_FIELDS: Array<keyof SiteConfigPayload> = [
  "siteTitleZh",
  "siteTitleJa",
  "siteTitleEn",
  "siteSubtitleZh",
  "siteSubtitleJa",
  "siteSubtitleEn",
  "aboutMdZh",
  "aboutMdJa",
  "aboutMdEn",
  "logoUrl",
  "faviconUrl",
  "icpNo",
  "spotifyPlaylistId",
  "startedDate"
];

function optional(value: string): string | null {
  const normalized = value.trim();
  return normalized ? normalized : null;
}

export function createSiteConfigForm(): SiteConfigForm {
  return SITE_CONFIG_FIELDS.reduce((form, field) => {
    form[field] = "";
    return form;
  }, {} as SiteConfigForm);
}

export function siteConfigToForm(config: SiteConfig): SiteConfigForm {
  const form = createSiteConfigForm();
  SITE_CONFIG_FIELDS.forEach(field => {
    form[field] = config[field] ?? "";
  });
  return form;
}

export function validateSiteConfigForm(
  form: SiteConfigForm
): SiteConfigFormErrors {
  const errors: SiteConfigFormErrors = {};
  if (!form.siteTitleZh.trim()) errors.siteTitleZh = "required";
  return errors;
}

export function siteConfigFormToPayload(
  form: SiteConfigForm
): SiteConfigPayload {
  return {
    siteTitleZh: form.siteTitleZh.trim(),
    siteTitleJa: form.siteTitleJa.trim(),
    siteTitleEn: form.siteTitleEn.trim(),
    siteSubtitleZh: optional(form.siteSubtitleZh),
    siteSubtitleJa: optional(form.siteSubtitleJa),
    siteSubtitleEn: optional(form.siteSubtitleEn),
    aboutMdZh: optional(form.aboutMdZh),
    aboutMdJa: optional(form.aboutMdJa),
    aboutMdEn: optional(form.aboutMdEn),
    logoUrl: optional(form.logoUrl),
    faviconUrl: optional(form.faviconUrl),
    icpNo: optional(form.icpNo),
    spotifyPlaylistId: optional(form.spotifyPlaylistId),
    startedDate: optional(form.startedDate)
  };
}
