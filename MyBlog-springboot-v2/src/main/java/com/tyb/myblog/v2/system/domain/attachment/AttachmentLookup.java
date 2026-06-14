package com.tyb.myblog.v2.system.domain.attachment;

/**
 * 包含软删除状态的附件查重结果。
 */
public record AttachmentLookup(Attachment attachment, boolean deleted) {
}
