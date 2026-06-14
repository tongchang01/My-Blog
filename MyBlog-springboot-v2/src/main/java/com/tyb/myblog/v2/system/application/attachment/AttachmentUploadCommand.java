package com.tyb.myblog.v2.system.application.attachment;

import java.io.InputStream;

/**
 * 附件上传应用命令。
 */
public record AttachmentUploadCommand(
        String originalFilename,
        InputStream inputStream
) {
}
