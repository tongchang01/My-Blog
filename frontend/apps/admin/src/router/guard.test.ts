import { describe, expect, it } from "vitest";
import { resolveGuardTarget } from "./guard";

describe("static admin route guard", () => {
  it("redirects anonymous visitors to login", () => {
    expect(resolveGuardTarget("/dashboard", null)).toBe("/login");
  });

  it("redirects authenticated visitors away from login", () => {
    expect(resolveGuardTarget("/login", "ADMIN")).toBe("/dashboard");
  });

  it("rejects a role excluded by route metadata", () => {
    expect(resolveGuardTarget("/settings", "DEMO", ["ADMIN"])).toBe(
      "/error/403"
    );
  });

  it("allows a role included by route metadata", () => {
    expect(resolveGuardTarget("/dashboard", "DEMO", ["ADMIN", "DEMO"])).toBe(
      true
    );
  });
});
