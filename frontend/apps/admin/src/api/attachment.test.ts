import MockAdapter from "axios-mock-adapter";
import { afterEach, describe, expect, it } from "vitest";
import { http } from "@/utils/http";
import {
  deleteAttachment,
  getAttachment,
  listDeletedAttachments,
  listAttachments,
  restoreAttachment,
  uploadAttachment
} from "./attachment";

const mock = new MockAdapter(http.instance);

afterEach(() => mock.reset());

describe("attachment API", () => {
  it("requests paged attachments with stable string ids", async () => {
    mock.onGet("/api/admin/attachments").reply(config => {
      expect(config.params).toEqual({ page: 2, size: 50 });
      return [
        200,
        {
          code: "00000",
          msg: "success",
          data: {
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
            page: 2,
            size: 50
          }
        }
      ];
    });

    await expect(listAttachments({ page: 2, size: 50 })).resolves.toMatchObject(
      {
        data: {
          records: [{ id: "9007199254743001", createdBy: "1001" }]
        }
      }
    );
  });

  it("requests attachment detail and multipart upload", async () => {
    mock.onGet("/api/admin/attachments/9007199254743001").reply(200, {
      code: "00000",
      msg: "success",
      data: {
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
    });

    mock.onPost("/api/admin/attachments").reply(config => {
      expect(config.data).toBeInstanceOf(FormData);
      expect((config.data as FormData).get("file")).toBeInstanceOf(File);
      expect(config.headers?.["Content-Type"]).toBe("multipart/form-data");
      return [
        200,
        {
          code: "00000",
          msg: "success",
          data: {
            id: "9007199254743002",
            publicUrl: "http://localhost/media/b.png",
            contentType: "image/png",
            fileSize: 3,
            width: 1,
            height: 1,
            originalFilename: "b.png",
            createdAt: "2026-06-25T12:01:00",
            createdBy: "1001"
          }
        }
      ];
    });

    await expect(getAttachment("9007199254743001")).resolves.toMatchObject({
      data: { id: "9007199254743001" }
    });
    await expect(
      uploadAttachment(new File(["png"], "b.png", { type: "image/png" }))
    ).resolves.toMatchObject({ data: { id: "9007199254743002" } });
  });

  it("requests deleted attachments, soft delete, and restore", async () => {
    mock.onGet("/api/admin/attachments/deleted").reply(config => {
      expect(config.params).toEqual({ page: 1, size: 20 });
      return [
        200,
        {
          code: "00000",
          msg: "success",
          data: { records: [], total: 0, page: 1, size: 20 }
        }
      ];
    });
    mock.onDelete("/api/admin/attachments/9007199254743001").reply(200, {
      code: "00000",
      msg: "success",
      data: null
    });
    mock.onPost("/api/admin/attachments/9007199254743001/restore").reply(200, {
      code: "00000",
      msg: "success",
      data: null
    });

    await expect(
      listDeletedAttachments({ page: 1, size: 20 })
    ).resolves.toMatchObject({ data: { total: 0 } });
    await expect(deleteAttachment("9007199254743001")).resolves.toMatchObject({
      data: null
    });
    await expect(restoreAttachment("9007199254743001")).resolves.toMatchObject({
      data: null
    });
  });
});
