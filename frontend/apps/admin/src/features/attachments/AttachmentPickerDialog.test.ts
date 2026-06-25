import MockAdapter from "axios-mock-adapter";
import { config, flushPromises, mount } from "@vue/test-utils";
import { afterEach, describe, expect, it } from "vitest";
import { http } from "@/utils/http";
import AttachmentPickerDialog from "./AttachmentPickerDialog.vue";

const mock = new MockAdapter(http.instance);
config.global.renderStubDefaultSlot = true;

const stubs = {
  "el-alert": true,
  "el-button": { template: "<button><slot /></button>" },
  "el-dialog": { template: "<div><slot /></div>" },
  "el-empty": true,
  "el-image": true,
  "el-pagination": true,
  "el-skeleton": true
};

const ok = (data: unknown) => ({ code: "00000", msg: "success", data });

function page() {
  return {
    records: [
      {
        id: "9007199254743001",
        publicUrl: "http://localhost/media/a.png",
        contentType: "image/png",
        fileSize: 1024,
        width: 800,
        height: 450,
        originalFilename: "a.png",
        createdAt: "2026-06-25T12:00:00",
        createdBy: "1001"
      }
    ],
    total: 1,
    page: 1,
    size: 20
  };
}

afterEach(() => mock.reset());

describe("attachment picker dialog", () => {
  it("loads attachments when opened and emits the selected item", async () => {
    mock.onGet("/api/admin/attachments").reply(200, ok(page()));
    const wrapper = mount(AttachmentPickerDialog, {
      props: { modelValue: true },
      global: { stubs }
    });
    await flushPromises();

    expect(mock.history.get[0].params).toEqual({ page: 1, size: 20 });
    expect(wrapper.text()).toContain("a.png");

    await wrapper.get('[data-testid="attachment-picker-select-9007199254743001"]').trigger("click");

    expect(wrapper.emitted("select")?.[0]?.[0]).toMatchObject({
      id: "9007199254743001",
      publicUrl: "http://localhost/media/a.png"
    });
    expect(wrapper.emitted("update:modelValue")?.[0]).toEqual([false]);
  });

  it("supports changing pages", async () => {
    mock.onGet("/api/admin/attachments").reply(200, ok(page()));
    const wrapper = mount(AttachmentPickerDialog, {
      props: { modelValue: true },
      global: {
        stubs: {
          ...stubs,
          "el-pagination": {
            template:
              "<button data-testid='next-page' @click=\"$emit('current-change', 2)\">next</button>"
          }
        }
      }
    });
    await flushPromises();

    await wrapper.get('[data-testid="next-page"]').trigger("click");
    await flushPromises();

    expect(mock.history.get.at(-1)?.params).toEqual({ page: 2, size: 20 });
  });
});
