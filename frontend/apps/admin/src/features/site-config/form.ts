import type { SiteConfig, SiteConfigPayload } from "./model";

export type SiteConfigForm = Record<keyof SiteConfigPayload, string>;

export type SiteConfigFormErrorCode =
  | "required"
  | "maxLength"
  | "url"
  | "spotifyId";

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
  validateMaxLength(errors, form, ["siteTitleZh", "siteTitleJa", "siteTitleEn"], 128);
  validateMaxLength(
    errors,
    form,
    ["siteSubtitleZh", "siteSubtitleJa", "siteSubtitleEn"],
    255
  );
  validateMaxLength(errors, form, ["aboutMdZh", "aboutMdJa", "aboutMdEn"], 50_000);
  validateMaxLength(errors, form, ["icpNo"], 64);
  validateMaxLength(errors, form, ["logoUrl", "faviconUrl"], 255);
  (["logoUrl", "faviconUrl"] as const).forEach(field => {
    if (form[field].trim() && !isHttpUrl(form[field].trim())) {
      errors[field] = "url";
    }
  });
  if (
    form.spotifyPlaylistId.trim() &&
    !/^[A-Za-z0-9_-]{1,64}$/.test(form.spotifyPlaylistId.trim())
  ) {
    errors.spotifyPlaylistId = "spotifyId";
  }
  return errors;
}

function validateMaxLength(
  errors: SiteConfigFormErrors,
  form: SiteConfigForm,
  fields: Array<keyof SiteConfigForm>,
  maxLength: number
): void {
  fields.forEach(field => {
    if (form[field].trim().length > maxLength) errors[field] = "maxLength";
  });
}

function isHttpUrl(value: string): boolean {
  try {
    const url = new URL(value);
    return (
      (url.protocol === "http:" || url.protocol === "https:") &&
      Boolean(url.hostname)
    );
  } catch {
    return false;
  }
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
