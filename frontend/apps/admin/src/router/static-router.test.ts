import { describe, expect, it } from "vitest";
import { localesConfigs } from "@/plugins/i18n";
import { constantMenus, resolveRouteDocumentTitle } from "./index";

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
    const articleRecycleBin = articles?.children?.find(
      route => route.name === "ArticleRecycleBin"
    );
    expect(articles?.path).toBe("/articles");
    expect(articleList?.meta?.roles).toEqual(["ADMIN", "DEMO"]);
    expect(articleRecycleBin?.path).toBe("/articles/recycle-bin");
    expect(articleRecycleBin?.meta?.showLink).toBe(true);
    expect(articleRecycleBin?.meta?.roles).toEqual(["ADMIN", "DEMO"]);
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
    const commentManagement = articles?.children?.find(
      route => route.name === "CommentManagement"
    );
    const friendLinkManagement = articles?.children?.find(
      route => route.name === "FriendLinkManagement"
    );
    expect(categoryList?.path).toBe("/categories/list");
    expect(categoryList?.meta?.roles).toEqual(["ADMIN", "DEMO"]);
    expect(tagList?.path).toBe("/tags/list");
    expect(tagList?.meta?.roles).toEqual(["ADMIN", "DEMO"]);
    expect(commentManagement?.path).toBe("/comments/list");
    expect(commentManagement?.meta?.showLink).toBe(true);
    expect(commentManagement?.meta?.roles).toEqual(["ADMIN", "DEMO"]);
    expect(friendLinkManagement?.path).toBe("/friend-links/list");
    expect(friendLinkManagement?.meta?.showLink).toBe(true);
    expect(friendLinkManagement?.meta?.roles).toEqual(["ADMIN", "DEMO"]);
  });

  it("provides article lifecycle labels in all admin locales", () => {
    for (const locale of [
      localesConfigs.zh,
      localesConfigs.ja,
      localesConfigs.en
    ]) {
      expect(locale.menus.articleRecycleBin).toBeTruthy();
      expect(locale.articles.actions.delete).toBeTruthy();
      expect(locale.articles.recycle.restore).toBeTruthy();
      expect(locale.articles.recycle.errors.referenceConflict).toBeTruthy();
      expect(locale.menus.commentManagement).toBeTruthy();
      expect(locale.comments.filter.title).toBeTruthy();
      expect(locale.comments.actions.approve).toBeTruthy();
      expect(locale.comments.errors.operation).toBeTruthy();
      expect(locale.menus.friendLinkManagement).toBeTruthy();
      expect(locale.friendLinks.filter.title).toBeTruthy();
      expect(locale.friendLinks.actions.saveSort).toBeTruthy();
      expect(locale.friendLinks.errors.operation).toBeTruthy();
    }
  });

  it("contains system settings routes for site config and profile", () => {
    const settings = (constantMenus as any[]).find(
      route => route.name === "Settings"
    );
    const siteConfig = settings?.children?.find(
      route => route.name === "SiteConfigManagement"
    );
    const profile = settings?.children?.find(
      route => route.name === "ProfileManagement"
    );

    expect(settings?.path).toBe("/settings");
    expect(siteConfig?.path).toBe("/settings/site-config");
    expect(siteConfig?.meta?.showLink).toBe(true);
    expect(siteConfig?.meta?.roles).toEqual(["ADMIN", "DEMO"]);
    expect(profile?.path).toBe("/settings/profile");
    expect(profile?.meta?.showLink).toBe(true);
    expect(profile?.meta?.roles).toEqual(["ADMIN", "DEMO"]);
  });

  it("provides settings labels in all admin locales", () => {
    for (const locale of [
      localesConfigs.zh,
      localesConfigs.ja,
      localesConfigs.en
    ]) {
      expect(locale.menus.systemManagement).toBeTruthy();
      expect(locale.menus.siteConfigManagement).toBeTruthy();
      expect(locale.menus.profileManagement).toBeTruthy();
      expect(locale.settings.readonlyDemo).toBeTruthy();
      expect(locale.settings.siteConfig.basic).toBeTruthy();
      expect(locale.settings.profile.form).toBeTruthy();
      expect(locale.settings.validation.required).toBeTruthy();
    }
  });

  it("uses the deepest matched route title for browser document title", () => {
    expect(
      resolveRouteDocumentTitle({
        matched: [
          { meta: { title: "menus.contentManagement" } },
          { meta: { title: "menus.commentManagement" } }
        ],
        meta: { title: "menus.commentManagement" }
      } as any)
    ).toBe("Comments");
  });
});
