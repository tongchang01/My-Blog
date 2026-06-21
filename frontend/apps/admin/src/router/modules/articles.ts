import { $t } from "@/plugins/i18n";
const Layout = () => import("@/layout/index.vue");

export default {
  path: "/articles",
  name: "Articles",
  component: Layout,
  redirect: "/articles/list",
  meta: {
    icon: "ep/document",
    title: $t("menus.articles"),
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
    }
  ]
} satisfies RouteConfigsTable;
