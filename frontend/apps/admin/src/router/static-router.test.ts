import { describe, expect, it } from "vitest";
import { constantMenus } from "./index";

describe("static admin routes", () => {
  it("contains dashboard without permission demo routes", () => {
    const text = JSON.stringify(constantMenus);
    expect(text).toContain("Dashboard");
    expect(text).not.toContain("PermissionPage");
    expect(text).not.toContain("PermissionButton");
  });

  it("contains the read-only article route for admin and demo roles", () => {
    const articles = (constantMenus as any[]).find(
      route => route.name === "Articles"
    );
    const articleList = articles?.children?.find(
      route => route.name === "ArticleList"
    );
    expect(articles?.path).toBe("/articles");
    expect(articleList?.meta?.roles).toEqual(["ADMIN", "DEMO"]);
    expect(JSON.stringify(articles)).not.toContain("edit");
  });
});
