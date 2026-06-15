package com.tyb.myblog.v2.content.application.tag;

/**
 * 完整编辑标签命令。
 */
public record UpdateTagCommand(
        String nameZh,
        String nameJa,
        String nameEn,
        String slug) {
}
