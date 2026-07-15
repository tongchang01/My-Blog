import { describe, expect, it, vi } from "vitest";

const { alert } = vi.hoisted(() => ({
  alert: vi.fn().mockResolvedValue(undefined)
}));

vi.mock("element-plus", () => ({ ElMessageBox: { alert } }));

import { showDemoLoginNotice } from "./demo-notice";

describe("demo login notice", () => {
  it("requires acknowledgement before a demo session continues", async () => {
    await showDemoLoginNotice(key => `translated:${key}`);

    expect(alert).toHaveBeenCalledWith(
      "translated:login.demoNotice.message",
      "translated:login.demoNotice.title",
      {
        type: "warning",
        confirmButtonText: "translated:login.demoNotice.confirm",
        closeOnClickModal: false,
        closeOnPressEscape: false,
        showClose: false
      }
    );
  });
});
