import { $t } from "@/plugins/i18n";
const Layout = () => import("@/layout/index.vue");

export default {
  path: "/",
  name: "Home",
  component: Layout,
  redirect: "/dashboard",
  meta: {
    icon: "ep/home-filled",
    title: $t("menus.dashboard"),
    rank: 0
  },
  children: [
    {
      path: "/dashboard",
      name: "Dashboard",
      component: () => import("@/features/dashboard/index.vue"),
      meta: {
        title: $t("menus.dashboard"),
        showLink: true,
        roles: ["ADMIN", "DEMO"]
      }
    }
  ]
} satisfies RouteConfigsTable;
