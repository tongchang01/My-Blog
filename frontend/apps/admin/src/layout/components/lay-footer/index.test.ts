import { flushPromises, mount } from "@vue/test-utils";
import { afterEach, describe, expect, it, vi } from "vitest";
import { setConfig } from "@/config";
import Footer from "./index.vue";

const { getSiteConfig } = vi.hoisted(() => ({ getSiteConfig: vi.fn() }));

vi.mock("@/api/site-config", () => ({ getSiteConfig }));

afterEach(() => vi.resetAllMocks());

describe("layout footer", () => {
  it("shows the MIIT record link only when site config provides an ICP number", async () => {
    setConfig({ Title: "MyBlog" });
    getSiteConfig.mockResolvedValue({ data: { icpNo: " 京ICP备12345678号 " } });

    const wrapper = mount(Footer);
    await flushPromises();

    expect(wrapper.text()).toContain("Copyright © 2020-present MyBlog");
    expect(wrapper.get('[data-testid="footer-icp"]').text()).toBe(
      "京ICP备12345678号"
    );
  });

  it("keeps the copyright when ICP configuration is empty", async () => {
    getSiteConfig.mockResolvedValue({ data: { icpNo: null } });

    const wrapper = mount(Footer);
    await flushPromises();

    expect(wrapper.find('[data-testid="footer-icp"]').exists()).toBe(false);
  });
});
