package com.tyb.myblog.v2.content.domain;

/**
 * 作者摘要。
 *
 * @param id       用户资料 ID
 * @param nickname 展示昵称
 * @param avatar   头像地址
 */
public record AuthorSummary(int id, String nickname, String avatar) {
}
