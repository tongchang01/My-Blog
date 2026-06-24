import { $t } from "@/plugins/i18n";
const Layout = () => import("@/layout/index.vue");

export default {
  path: "/articles",
  name: "Articles",
  component: Layout,
  redirect: "/articles/list",
  meta: {
    icon: "ep/document",
    title: $t("menus.contentManagement"),
    rank: 1
  },
  children: [
    {
      path: "/articles/list",
      name: "ArticleList",
      component: () => import("@/features/articles/index.vue"),
      meta: {
        title: $t("menus.articleManagement"),
        showLink: true,
        roles: ["ADMIN", "DEMO"]
      }
    },
    {
      path: "/articles/recycle-bin",
      name: "ArticleRecycleBin",
      component: () =>
        import("@/features/articles/recycle-bin/index.vue"),
      meta: {
        title: $t("menus.articleRecycleBin"),
        showLink: true,
        roles: ["ADMIN", "DEMO"]
      }
    },
    {
      path: "/comments/list",
      name: "CommentManagement",
      component: () => import("@/features/comments/index.vue"),
      meta: {
        title: $t("menus.commentManagement"),
        showLink: true,
        roles: ["ADMIN", "DEMO"]
      }
    },
    {
      path: "/friend-links/list",
      name: "FriendLinkManagement",
      component: () => import("@/features/friend-links/index.vue"),
      meta: {
        title: $t("menus.friendLinkManagement"),
        showLink: true,
        roles: ["ADMIN", "DEMO"]
      }
    },
    {
      path: "/articles/new",
      name: "ArticleCreate",
      component: () => import("@/features/articles/editor/index.vue"),
      meta: {
        title: $t("articles.editor.createTitle"),
        showLink: false,
        activePath: "/articles/list",
        roles: ["ADMIN"]
      }
    },
    {
      path: "/articles/:id/edit",
      name: "ArticleEdit",
      component: () => import("@/features/articles/editor/index.vue"),
      meta: {
        title: $t("articles.editor.editTitle"),
        showLink: false,
        activePath: "/articles/list",
        roles: ["ADMIN"]
      }
    },
    {
      path: "/categories/list",
      name: "CategoryList",
      component: () =>
        import("@/features/taxonomy/categories/index.vue"),
      meta: {
        title: $t("menus.categoryManagement"),
        showLink: true,
        roles: ["ADMIN", "DEMO"]
      }
    },
    {
      path: "/tags/list",
      name: "TagList",
      component: () => import("@/features/taxonomy/tags/index.vue"),
      meta: {
        title: $t("menus.tagManagement"),
        showLink: true,
        roles: ["ADMIN", "DEMO"]
      }
    }
  ]
} satisfies RouteConfigsTable;
