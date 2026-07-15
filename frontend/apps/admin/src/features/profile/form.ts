import type { UserProfile } from "@/features/auth/model";

export interface UserProfilePayload {
  nickname: string;
  avatarUrl: string | null;
  bioZh: string | null;
  bioJa: string | null;
  bioEn: string | null;
  location: string | null;
  website: string | null;
  emailPublic: string | null;
  githubUrl: string | null;
  twitterUrl: string | null;
  linkedinUrl: string | null;
  zhihuUrl: string | null;
  qiitaUrl: string | null;
  juejinUrl: string | null;
}

export type UserProfileForm = Record<keyof UserProfilePayload, string>;

export type UserProfileFormErrorCode =
  | "required"
  | "maxLength"
  | "url"
  | "email";

export type UserProfileFormErrors = Partial<
  Record<keyof UserProfileForm, UserProfileFormErrorCode>
>;

const PROFILE_FIELDS: Array<keyof UserProfilePayload> = [
  "nickname",
  "avatarUrl",
  "bioZh",
  "bioJa",
  "bioEn",
  "location",
  "website",
  "emailPublic",
  "githubUrl",
  "twitterUrl",
  "linkedinUrl",
  "zhihuUrl",
  "qiitaUrl",
  "juejinUrl"
];

function optional(value: string): string | null {
  const normalized = value.trim();
  return normalized ? normalized : null;
}

export function createUserProfileForm(): UserProfileForm {
  return PROFILE_FIELDS.reduce((form, field) => {
    form[field] = "";
    return form;
  }, {} as UserProfileForm);
}

export function userProfileToForm(profile: UserProfile): UserProfileForm {
  const form = createUserProfileForm();
  PROFILE_FIELDS.forEach(field => {
    form[field] = profile[field] ?? "";
  });
  return form;
}

export function validateUserProfileForm(
  form: UserProfileForm
): UserProfileFormErrors {
  const errors: UserProfileFormErrors = {};
  if (!form.nickname.trim()) errors.nickname = "required";
  validateMaxLength(errors, form, ["nickname", "location"], 64);
  validateMaxLength(errors, form, ["bioZh", "bioJa", "bioEn"], 5_000);
  validateMaxLength(errors, form, ["emailPublic"], 128);
  const urlFields: Array<keyof UserProfileForm> = [
    "avatarUrl",
    "website",
    "githubUrl",
    "twitterUrl",
    "linkedinUrl",
    "zhihuUrl",
    "qiitaUrl",
    "juejinUrl"
  ];
  validateMaxLength(errors, form, urlFields, 255);
  urlFields.forEach(field => {
    if (form[field].trim() && !isHttpUrl(form[field].trim())) {
      errors[field] = "url";
    }
  });
  if (
    form.emailPublic.trim() &&
    !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.emailPublic.trim())
  ) {
    errors.emailPublic = "email";
  }
  return errors;
}

function validateMaxLength(
  errors: UserProfileFormErrors,
  form: UserProfileForm,
  fields: Array<keyof UserProfileForm>,
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

export function userProfileFormToPayload(
  form: UserProfileForm
): UserProfilePayload {
  return {
    nickname: form.nickname.trim(),
    avatarUrl: optional(form.avatarUrl),
    bioZh: optional(form.bioZh),
    bioJa: optional(form.bioJa),
    bioEn: optional(form.bioEn),
    location: optional(form.location),
    website: optional(form.website),
    emailPublic: optional(form.emailPublic),
    githubUrl: optional(form.githubUrl),
    twitterUrl: optional(form.twitterUrl),
    linkedinUrl: optional(form.linkedinUrl),
    zhihuUrl: optional(form.zhihuUrl),
    qiitaUrl: optional(form.qiitaUrl),
    juejinUrl: optional(form.juejinUrl)
  };
}
