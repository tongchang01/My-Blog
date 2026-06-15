package com.tyb.myblog.v2.content.application.tag;

/**
 * 新增标签命令。
 */
public record CreateTagCommand(
        String nameZh,
        String nameJa,
        String nameEn,
        String slug) {
}
