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
    }
  ]
} satisfies RouteConfigsTable;
