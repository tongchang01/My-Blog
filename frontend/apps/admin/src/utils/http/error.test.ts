import { describe, expect, it } from "vitest";
import { apiErrorFromCode } from "./error";

describe("apiErrorFromCode", () => {
  it.each([
    ["10001", "badCredentials"],
    ["10002", "sessionExpired"],
    ["10003", "forbidden"],
    ["90001", "validation"],
    ["90002", "rateLimited"],
    ["99999", "server"],
    ["unexpected", "unknown"]
  ] as const)("maps %s to %s", (code, kind) => {
    expect(apiErrorFromCode(code).kind).toBe(kind);
  });
});
