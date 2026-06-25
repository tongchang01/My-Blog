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

export type UserProfileFormErrorCode = "required";

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
  return errors;
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
