import { describe, expect, it } from "vitest";
import { clearSession, loadSession, saveSession } from "./session-storage";

const futureSession = () => ({
  accessToken: "access",
  refreshToken: "refresh",
  accessExpiresAt: Date.now() + 60_000,
  refreshExpiresAt: Date.now() + 120_000
});

describe("session storage", () => {
  it("round-trips and clears a session", () => {
    const session = futureSession();
    saveSession(session);
    expect(loadSession()).toEqual(session);
    clearSession();
    expect(loadSession()).toBeNull();
  });

  it("removes malformed persisted data", () => {
    localStorage.setItem("myblog-admin-session", "not-json");
    expect(loadSession()).toBeNull();
    expect(localStorage.getItem("myblog-admin-session")).toBeNull();
  });

  it("removes a session whose refresh token has expired", () => {
    saveSession({ ...futureSession(), refreshExpiresAt: Date.now() - 1 });
    expect(loadSession()).toBeNull();
  });
});
