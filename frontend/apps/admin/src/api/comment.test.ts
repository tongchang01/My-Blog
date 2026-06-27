import MockAdapter from "axios-mock-adapter";
import { afterEach, describe, expect, it } from "vitest";
import { http } from "@/utils/http";
import {
  approveComment,
  deleteComment,
  hideComment,
  listComments,
  replyComment,
  restoreComment
} from "./comment";

const mock = new MockAdapter(http.instance);

afterEach(() => mock.reset());

describe("comment API", () => {
  it("requests the admin comment page with normalized params", async () => {
    mock.onGet("/api/admin/comments").reply(config => {
      expect(config.params).toEqual({
        targetType: "ARTICLE",
        targetId: "9007199254740993",
        auditStatus: "PENDING",
        keyword: "hello",
        includeDeleted: true,
        page: 2,
        size: 50
      });
      return [
        200,
        {
          code: "00000",
          msg: "success",
          data: {
            records: [
              {
                id: "9007199254740995",
                targetType: "ARTICLE",
                targetId: "9007199254740993",
                parentId: null,
                replyToCommentId: null,
                replyToNickname: null,
                authorNickname: "TYB",
                authorEmail: "tyb@example.com",
                authorSite: "https://example.com",
                authorIp: "127.0.0.1",
                authorUserAgent: "Vitest",
                contentMd: "hello",
                contentHtml: "<p>hello</p>",
                auditStatus: "PENDING",
                createdAt: "2026-06-23T12:00:00",
                deleted: false
              }
            ],
            total: 1,
            page: 2,
            size: 50
          }
        }
      ];
    });

    await expect(
      listComments({
        targetType: "ARTICLE",
        targetId: " 9007199254740993 ",
        auditStatus: "PENDING",
        keyword: " hello ",
        includeDeleted: true,
        page: 2,
        size: 50
      })
    ).resolves.toMatchObject({
      data: {
        records: [
          {
            id: "9007199254740995",
            targetId: "9007199254740993"
          }
        ]
      }
    });
  });

  it("requests moderation command endpoints", async () => {
    mock.onPost("/api/admin/comments/9007199254740995/approve").reply(200, {
      code: "00000",
      msg: "success",
      data: null
    });
    mock.onPost("/api/admin/comments/9007199254740995/hide").reply(200, {
      code: "00000",
      msg: "success",
      data: null
    });
    mock.onPost("/api/admin/comments/9007199254740995/restore").reply(200, {
      code: "00000",
      msg: "success",
      data: null
    });
    mock.onDelete("/api/admin/comments/9007199254740995").reply(200, {
      code: "00000",
      msg: "success",
      data: null
    });

    await expect(approveComment("9007199254740995")).resolves.toMatchObject({
      data: null
    });
    await expect(hideComment("9007199254740995")).resolves.toMatchObject({
      data: null
    });
    await expect(restoreComment("9007199254740995")).resolves.toMatchObject({
      data: null
    });
    await expect(deleteComment("9007199254740995")).resolves.toMatchObject({
      data: null
    });
  });

  it("requests the admin reply endpoint with markdown content", async () => {
    mock.onPost("/api/admin/comments/9007199254740995/reply").reply(200, {
      code: "00000",
      msg: "success",
      data: {
        id: "9007199254740997",
        auditStatus: "PASS"
      }
    });

    await expect(
      replyComment("9007199254740995", " 谢谢反馈 ")
    ).resolves.toMatchObject({
      data: {
        id: "9007199254740997",
        auditStatus: "PASS"
      }
    });
    expect(mock.history.post[0].url).toBe(
      "/api/admin/comments/9007199254740995/reply"
    );
    expect(JSON.parse(mock.history.post[0].data)).toEqual({
      contentMd: " 谢谢反馈 "
    });
  });
});
