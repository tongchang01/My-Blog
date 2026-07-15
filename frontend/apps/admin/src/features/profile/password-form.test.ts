import { describe, expect, it } from "vitest";
import {
  createPasswordForm,
  passwordFormToPayload,
  validatePasswordForm,
  type PasswordForm
} from "./password-form";

describe("password form", () => {
  it("creates an empty form and validates required fields", () => {
    const form = createPasswordForm();

    expect(form).toEqual({
      currentPassword: "",
      newPassword: "",
      confirmPassword: ""
    });
    expect(validatePasswordForm(form)).toEqual({
      currentPassword: "required",
      newPassword: "required",
      confirmPassword: "required"
    });
  });

  it("requires an 8 to 128 character new password and matching confirmation", () => {
    const form: PasswordForm = {
      currentPassword: "old-password",
      newPassword: "short",
      confirmPassword: "different"
    };

    expect(validatePasswordForm(form)).toEqual({
      newPassword: "length",
      confirmPassword: "mismatch"
    });
  });

  it("rejects a current password longer than the backend limit", () => {
    expect(
      validatePasswordForm({
        currentPassword: "x".repeat(129),
        newPassword: "new-password",
        confirmPassword: "new-password"
      })
    ).toEqual({ currentPassword: "currentLength" });
  });

  it("does not allow reusing the current password", () => {
    expect(
      validatePasswordForm({
        currentPassword: "same-password",
        newPassword: "same-password",
        confirmPassword: "same-password"
      })
    ).toEqual({ newPassword: "sameAsCurrent" });
  });

  it("trims only the current password payload boundary", () => {
    const form: PasswordForm = {
      currentPassword: " old-password ",
      newPassword: "new-password",
      confirmPassword: "new-password"
    };

    expect(passwordFormToPayload(form)).toEqual({
      currentPassword: " old-password ",
      newPassword: "new-password"
    });
  });
});
