import type { ChangePasswordPayload } from "@/api/auth";

export interface PasswordForm {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}

export type PasswordFormErrorCode = "required" | "length" | "mismatch";

export type PasswordFormErrors = Partial<
  Record<keyof PasswordForm, PasswordFormErrorCode>
>;

export function createPasswordForm(): PasswordForm {
  return {
    currentPassword: "",
    newPassword: "",
    confirmPassword: ""
  };
}

export function validatePasswordForm(form: PasswordForm): PasswordFormErrors {
  const errors: PasswordFormErrors = {};
  if (!form.currentPassword.trim()) errors.currentPassword = "required";
  if (!form.newPassword.trim()) {
    errors.newPassword = "required";
  } else if (form.newPassword.length < 8 || form.newPassword.length > 128) {
    errors.newPassword = "length";
  }
  if (!form.confirmPassword.trim()) {
    errors.confirmPassword = "required";
  } else if (form.newPassword !== form.confirmPassword) {
    errors.confirmPassword = "mismatch";
  }
  return errors;
}

export function passwordFormToPayload(form: PasswordForm): ChangePasswordPayload {
  return {
    currentPassword: form.currentPassword,
    newPassword: form.newPassword
  };
}
