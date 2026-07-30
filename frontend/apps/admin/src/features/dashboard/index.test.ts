import MockAdapter from "axios-mock-adapter";
import { config, flushPromises, mount } from "@vue/test-utils";
import { afterEach, describe, expect, it, vi } from "vitest";
import { localesConfigs, transformI18n } from "@/plugins/i18n";
import { useUserStoreHook } from "@/store/modules/user";
import { http } from "@/utils/http";
import Dashboard from "./index.vue";

const mocks = vi.hoisted(() => {
  const chart = {
    dispose: vi.fn(),
    resize: vi.fn(),
    setOption: vi.fn()
  };
  return {
    chart,
    chartInit: vi.fn(() => chart),
    getChart: vi.fn(() => chart),
    observeResize: vi.fn(),
    routerPush: vi.fn()
  };
});

vi.mock("@vueuse/core", () => ({
  useResizeObserver: mocks.observeResize
}));
vi.mock("vue-router", () => ({
  useRouter: () => ({ push: mocks.routerPush })
}));
vi.mock("@/plugins/echarts", () => ({
  default: {
    getInstanceByDom: mocks.getChart,
    init: mocks.chartInit
  }
}));

const userStore = useUserStoreHook();
const mock = new MockAdapter(http.instance);
config.global.renderStubDefaultSlot = true;

const ok = (data: unknown) => ({ code: "00000", msg: "success", data });

const statsDashboard = {
  periodPv: 1234,
  todayPv: 56,
  todayUv: 34,
  averageDailyUv: 12.3,
  trend: [{ date: "2026-06-25", pv: 100, uv: 20 }],
  topArticles: [
    {
      articleId: "9007199254743001",
      title: "文章 A",
      pv: 88,
      dailyUvSum: 30
    }
  ],
  languageDistribution: [{ language: "zh", pv: 800, ratio: 0.648 }]
};

const emptyStatsDashboard = {
  periodPv: 0,
  todayPv: 0,
  todayUv: 0,
  averageDailyUv: 0,
  trend: [],
  topArticles: [],
  languageDistribution: []
};

const stubs = {
  "el-alert": true,
  "el-button": { template: "<button><slot /></button>" },
  "el-card": { template: "<div><slot name='header' /><slot /></div>" },
  "el-date-picker": true,
  "el-empty": true,
  "el-skeleton": true,
  "el-statistic": {
    props: ["value"],
    template: "<div><slot name='title' />{{ value }}<slot /></div>"
  }
};

function setUser(type: "ADMIN" | "DEMO" = "ADMIN") {
  userStore.SET_CURRENT_USER({
    id: "1",
    username: type.toLowerCase(),
    type,
    profile: {
      nickname: type === "DEMO" ? "Demo" : "Admin",
      avatarUrl: null,
      bioZh: null,
      bioJa: null,
      bioEn: null,
      location: null,
      website: null,
      emailPublic: null,
      githubUrl: null,
      twitterUrl: null,
      linkedinUrl: null,
      zhihuUrl: null,
      qiitaUrl: null,
      juejinUrl: null
    }
  });
}

afterEach(() => {
  mock.reset();
  vi.clearAllMocks();
  userStore.CLEAR_USER();
});

describe("admin dashboard", () => {
  it("shows the localized read-only marker for a demo account", () => {
    setUser("DEMO");
    mock.onGet("/api/admin/stats/dashboard").reply(200, ok(statsDashboard));

    const wrapper = mount(Dashboard, {
      global: {
        stubs
      }
    });

    expect(
      wrapper.get('[data-testid="demo-read-only"]').attributes("title")
    ).not.toBe("");
    expect(localesConfigs.zh.status.readOnlyDemo).toBeTruthy();
    expect(localesConfigs.ja.status.readOnlyDemo).toBeTruthy();
    expect(localesConfigs.en.status.readOnlyDemo).toBeTruthy();
  });

  it("loads and renders real stats dashboard data", async () => {
    setUser("ADMIN");
    mock.onGet("/api/admin/stats/dashboard").reply(200, ok(statsDashboard));

    const wrapper = mount(Dashboard, { global: { stubs } });
    await flushPromises();

    expect(
      wrapper.get('[data-testid="dashboard-metric-period-pv"]').text()
    ).toContain("1234");
    expect(
      wrapper.get('[data-testid="dashboard-period-metrics"]').text()
    ).toContain(transformI18n("dashboard.sections.period"));
    expect(
      wrapper.get('[data-testid="dashboard-today-metrics"]').text()
    ).toContain(transformI18n("dashboard.sections.today"));
    expect(
      wrapper.get('[data-testid="dashboard-metric-today-pv"]').text()
    ).toContain("56");
    expect(
      wrapper.get('[data-testid="dashboard-metric-today-uv"]').text()
    ).toContain("34");
    expect(
      wrapper.get('[data-testid="dashboard-metric-today-uv"]').text()
    ).toContain(transformI18n("dashboard.metrics.todayUv"));
    expect(
      wrapper.find('[data-testid="dashboard-metric-average-daily-uv"]').exists()
    ).toBe(false);
    expect(
      wrapper
        .get('[data-testid="dashboard-top-article-9007199254743001"]')
        .text()
    ).toContain("文章 A");
    expect(
      wrapper.get('[data-testid="dashboard-language-zh"]').text()
    ).toContain("800");
    expect(wrapper.find('[data-testid="dashboard-trend-chart"]').exists()).toBe(
      true
    );
    expect(mock.history.get[0].params).toEqual({
      from: undefined,
      to: undefined
    });
    expect(
      wrapper.find('[data-testid="dashboard-default-period"]').exists()
    ).toBe(true);
    expect(
      wrapper
        .get('[data-testid="dashboard-top-article-9007199254743001"]')
        .text()
    ).toContain("UV");
    await wrapper
      .get('[data-testid="dashboard-top-article-link-9007199254743001"]')
      .trigger("click");
    expect(mocks.routerPush).toHaveBeenCalledWith(
      "/articles/9007199254743001/edit"
    );

    const resize = mocks.observeResize.mock.calls[0][1] as () => void;
    resize();
    expect(mocks.chart.resize).toHaveBeenCalledOnce();
  });

  it("keeps zero metrics and shows local empty states", async () => {
    setUser("ADMIN");
    mock
      .onGet("/api/admin/stats/dashboard")
      .reply(200, ok(emptyStatsDashboard));

    const wrapper = mount(Dashboard, { global: { stubs } });
    await flushPromises();

    expect(
      wrapper.get('[data-testid="dashboard-metric-period-pv"]').text()
    ).toContain("0");
    expect(
      wrapper.get('[data-testid="dashboard-metric-today-pv"]').text()
    ).toContain("0");
    expect(
      wrapper.get('[data-testid="dashboard-metric-today-uv"]').text()
    ).toContain("0");
    expect(wrapper.find('[data-testid="dashboard-empty"]').exists()).toBe(
      false
    );
    expect(wrapper.find('[data-testid="dashboard-trend-empty"]').exists()).toBe(
      true
    );
    expect(
      wrapper.find('[data-testid="dashboard-top-articles-empty"]').exists()
    ).toBe(true);
    expect(
      wrapper.find('[data-testid="dashboard-language-empty"]').exists()
    ).toBe(true);
  });

  it("shows a load error and retries stats dashboard loading", async () => {
    setUser("ADMIN");
    mock
      .onGet("/api/admin/stats/dashboard")
      .replyOnce(500)
      .onGet("/api/admin/stats/dashboard")
      .reply(200, ok(statsDashboard));

    const wrapper = mount(Dashboard, { global: { stubs } });
    await flushPromises();

    expect(wrapper.find('[data-testid="dashboard-error"]').exists()).toBe(true);
    await wrapper.get('[data-testid="dashboard-retry"]').trigger("click");
    await flushPromises();

    expect(wrapper.find('[data-testid="dashboard-error"]').exists()).toBe(
      false
    );
    expect(
      wrapper.find('[data-testid="dashboard-metric-period-pv"]').exists()
    ).toBe(true);
    expect(mock.history.get).toHaveLength(2);
  });
});
