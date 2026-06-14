package com.tyb.myblog.v2.system.application.attachment;

import java.util.List;

/**
 * 附件应用层分页结果，不依赖 Web 响应类型。
 */
public record AttachmentPageResult(
        List<AttachmentResult> records,
        long total,
        int page,
        int size
) {
    public AttachmentPageResult {
        records = records == null ? List.of() : List.copyOf(records);
    }
}
