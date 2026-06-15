package com.tyb.myblog.v2.system.application.attachment;

/**
 * 其它业务模块引用附件时可见的最小结果。
 */
public record AttachmentReferenceResult(
        long id,
        String publicUrl,
        String contentType) {
}
