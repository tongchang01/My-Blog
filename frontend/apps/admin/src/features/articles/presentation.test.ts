import { describe, expect, it } from "vitest";
import {
  formatJstDateTime,
  localizedName,
  statusTranslationKey
} from "./presentation";

describe("article presentation", () => {
  const names = {
    nameZh: "中文",
    nameJa: "日本語",
    nameEn: "English"
  };

  it("selects the requested locale then Chinese then any non-empty name", () => {
    expect(localizedName(names, "ja")).toBe("日本語");
    expect(localizedName({ ...names, nameJa: null }, "ja")).toBe("中文");
    expect(
      localizedName(
        { nameZh: null, nameJa: "日本語", nameEn: "English" },
        "en"
      )
    ).toBe("English");
    expect(
      localizedName({ nameZh: null, nameJa: null, nameEn: null }, "zh")
    ).toBe("—");
  });

  it("maps known and unknown status values to translation keys", () => {
    expect(statusTranslationKey("DRAFT")).toBe("articles.status.draft");
    expect(statusTranslationKey("UNEXPECTED")).toBe(
      "articles.status.unknown"
    );
  });

  it("formats JST local text without browser timezone conversion", () => {
    expect(formatJstDateTime("2026-06-21T09:05:00")).toBe(
      "2026-06-21 09:05"
    );
    expect(formatJstDateTime(null)).toBe("—");
  });
});
