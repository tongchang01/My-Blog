import type {
  FriendLinkItem,
  FriendLinkStatus,
  FriendLinkWritePayload
} from "./model";

export type FriendLinkFormErrorCode =
  | "required"
  | "url"
  | "sortOrderRange";

export interface FriendLinkForm {
  name: string;
  url: string;
  avatarUrl: string;
  description: string;
  sortOrder: number;
  status: FriendLinkStatus;
}

export type FriendLinkFormErrors = Partial<
  Record<keyof FriendLinkForm, FriendLinkFormErrorCode>
>;

export function createFriendLinkForm(): FriendLinkForm {
  return {
    name: "",
    url: "",
    avatarUrl: "",
    description: "",
    sortOrder: 0,
    status: "VISIBLE"
  };
}

export function friendLinkToForm(item: FriendLinkItem): FriendLinkForm {
  return {
    name: item.name,
    url: item.url,
    avatarUrl: item.avatarUrl ?? "",
    description: item.description ?? "",
    sortOrder: item.sortOrder,
    status: item.status
  };
}

export function validateFriendLinkForm(
  form: FriendLinkForm
): FriendLinkFormErrors {
  const errors: FriendLinkFormErrors = {};
  if (!form.name.trim()) errors.name = "required";
  const url = form.url.trim();
  if (!url) {
    errors.url = "required";
  } else if (!isHttpUrl(url)) {
    errors.url = "url";
  }
  if (form.avatarUrl.trim() && !isHttpUrl(form.avatarUrl.trim())) {
    errors.avatarUrl = "url";
  }
  if (
    !Number.isInteger(form.sortOrder) ||
    form.sortOrder < 0 ||
    form.sortOrder > 1_000_000
  ) {
    errors.sortOrder = "sortOrderRange";
  }
  return errors;
}

export function friendLinkFormToPayload(
  form: FriendLinkForm
): FriendLinkWritePayload {
  return {
    name: form.name.trim(),
    url: form.url.trim(),
    avatarUrl: optional(form.avatarUrl),
    description: optional(form.description),
    sortOrder: form.sortOrder,
    status: form.status
  };
}

function optional(value: string): string | null {
  const normalized = value.trim();
  return normalized || null;
}

function isHttpUrl(value: string): boolean {
  try {
    const url = new URL(value);
    return url.protocol === "http:" || url.protocol === "https:";
  } catch {
    return false;
  }
}
