package com.tyb.myblog.v2.content.web;

import com.tyb.myblog.v2.content.application.tag.UpdateTagCommand;

/**
 * 完整编辑标签 HTTP 请求。
 */
public class UpdateTagRequest extends TagWriteRequestSupport {

    public UpdateTagCommand toCommand() {
        Values values = values();
        return new UpdateTagCommand(
                values.nameZh(),
                values.nameJa(),
                values.nameEn(),
                values.slug());
    }
}
