package com.tyb.myblog.v2.system.domain.attachment;

import java.util.List;

/**
 * 附件领域分页结果。
 */
public record AttachmentPage(
        List<Attachment> records,
        long total,
        int page,
        int size
) {
    public AttachmentPage {
        records = records == null ? List.of() : List.copyOf(records);
    }
}
