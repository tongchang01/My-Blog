import { $t } from "@/plugins/i18n";
const Layout = () => import("@/layout/index.vue");

export default {
  path: "/settings",
  name: "Settings",
  component: Layout,
  redirect: "/settings/site-config",
  meta: {
    icon: "ep/setting",
    title: $t("menus.systemManagement"),
    rank: 2
  },
  children: [
    {
      path: "/settings/site-config",
      name: "SiteConfigManagement",
      component: () => import("@/features/site-config/index.vue"),
      meta: {
        title: $t("menus.siteConfigManagement"),
        showLink: true,
        roles: ["ADMIN", "DEMO"]
      }
    },
    {
      path: "/settings/profile",
      name: "ProfileManagement",
      component: () => import("@/features/profile/index.vue"),
      meta: {
        title: $t("menus.profileManagement"),
        showLink: true,
        roles: ["ADMIN", "DEMO"]
      }
    }
  ]
} satisfies RouteConfigsTable;
