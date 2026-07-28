import { describe, expect, it } from "vitest";
import { formatFileSize } from "./presentation";

describe("attachment presentation", () => {
  it.each([
    [1023, "1023 B"],
    [1024, "1 KB"],
    [1536, "1.5 KB"],
    [10 * 1024, "10 KB"],
    [1024 * 1024, "1 MB"],
    [1.5 * 1024 * 1024, "1.5 MB"]
  ])("formats %d bytes as %s", (bytes, expected) => {
    expect(formatFileSize(bytes)).toBe(expected);
  });
});
