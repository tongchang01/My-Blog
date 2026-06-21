import { describe, expect, it } from "vitest";
import { constantMenus } from "./index";

describe("static admin routes", () => {
  it("contains dashboard without permission demo routes", () => {
    const text = JSON.stringify(constantMenus);
    expect(text).toContain("Dashboard");
    expect(text).not.toContain("PermissionPage");
    expect(text).not.toContain("PermissionButton");
  });

  it("keeps the list readable and protects article write routes", () => {
    const articles = (constantMenus as any[]).find(
      route => route.name === "Articles"
    );
    const articleList = articles?.children?.find(
      route => route.name === "ArticleList"
    );
    expect(articles?.path).toBe("/articles");
    expect(articleList?.meta?.roles).toEqual(["ADMIN", "DEMO"]);
    const articleCreate = articles?.children?.find(
      route => route.name === "ArticleCreate"
    );
    const articleEdit = articles?.children?.find(
      route => route.name === "ArticleEdit"
    );
    expect(articleCreate?.meta?.roles).toEqual(["ADMIN"]);
    expect(articleCreate?.meta?.showLink).toBe(false);
    expect(articleEdit?.path).toBe("/articles/:id/edit");
    expect(articleEdit?.meta?.roles).toEqual(["ADMIN"]);
    const categoryList = articles?.children?.find(
      route => route.name === "CategoryList"
    );
    const tagList = articles?.children?.find(
      route => route.name === "TagList"
    );
    expect(categoryList?.path).toBe("/categories/list");
    expect(categoryList?.meta?.roles).toEqual(["ADMIN", "DEMO"]);
    expect(tagList?.path).toBe("/tags/list");
    expect(tagList?.meta?.roles).toEqual(["ADMIN", "DEMO"]);
  });
});
