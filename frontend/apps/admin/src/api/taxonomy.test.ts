import MockAdapter from "axios-mock-adapter";
import { afterEach, describe, expect, it } from "vitest";
import type {
  CategoryWritePayload,
  TagWritePayload
} from "@/features/taxonomy/model";
import { http } from "@/utils/http";
import {
  createCategory,
  createTag,
  deleteCategory,
  deleteTag,
  getCategory,
  getTag,
  listCategories,
  listTags,
  updateCategory,
  updateCategorySortOrders,
  updateTag
} from "./taxonomy";

const mock = new MockAdapter(http.instance);

afterEach(() => mock.reset());

const ok = (data: unknown = null) => ({
  code: "00000",
  msg: "success",
  data
});

describe("taxonomy API", () => {
  it("requests category reads and complete writes", async () => {
    const payload: CategoryWritePayload = {
      nameZh: "后端",
      nameJa: null,
      nameEn: "Backend",
      slug: "backend",
      sortOrder: 20
    };
    mock.onGet("/api/admin/categories").reply(200, ok([]));
    mock.onGet("/api/admin/categories/100").reply(200, ok({ id: "100" }));
    mock.onPost("/api/admin/categories").reply(config => {
      expect(JSON.parse(config.data)).toEqual(payload);
      return [200, ok({ id: "101" })];
    });
    mock.onPut("/api/admin/categories/100").reply(config => {
      expect(JSON.parse(config.data)).toEqual(payload);
      return [200, ok({ id: "100" })];
    });
    mock.onPut("/api/admin/categories/sort-orders").reply(config => {
      expect(JSON.parse(config.data)).toEqual({
        items: [{ id: "100", sortOrder: 30 }]
      });
      return [200, ok()];
    });
    mock.onDelete("/api/admin/categories/100").reply(200, ok());

    await expect(listCategories()).resolves.toMatchObject({ data: [] });
    await expect(getCategory("100")).resolves.toMatchObject({
      data: { id: "100" }
    });
    await expect(createCategory(payload)).resolves.toMatchObject({
      data: { id: "101" }
    });
    await expect(updateCategory("100", payload)).resolves.toMatchObject({
      data: { id: "100" }
    });
    await expect(
      updateCategorySortOrders([{ id: "100", sortOrder: 30 }])
    ).resolves.toMatchObject({ code: "00000" });
    await expect(deleteCategory("100")).resolves.toMatchObject({
      code: "00000"
    });
  });

  it("requests tag reads and complete writes", async () => {
    const payload: TagWritePayload = {
      nameZh: "Vue",
      nameJa: null,
      nameEn: "Vue",
      slug: "vue"
    };
    mock.onGet("/api/admin/tags").reply(200, ok([]));
    mock.onGet("/api/admin/tags/200").reply(200, ok({ id: "200" }));
    mock.onPost("/api/admin/tags").reply(config => {
      expect(JSON.parse(config.data)).toEqual(payload);
      return [200, ok({ id: "201" })];
    });
    mock.onPut("/api/admin/tags/200").reply(config => {
      expect(JSON.parse(config.data)).toEqual(payload);
      return [200, ok({ id: "200" })];
    });
    mock.onDelete("/api/admin/tags/200").reply(200, ok());

    await expect(listTags()).resolves.toMatchObject({ data: [] });
    await expect(getTag("200")).resolves.toMatchObject({
      data: { id: "200" }
    });
    await expect(createTag(payload)).resolves.toMatchObject({
      data: { id: "201" }
    });
    await expect(updateTag("200", payload)).resolves.toMatchObject({
      data: { id: "200" }
    });
    await expect(deleteTag("200")).resolves.toMatchObject({
      code: "00000"
    });
  });
});
