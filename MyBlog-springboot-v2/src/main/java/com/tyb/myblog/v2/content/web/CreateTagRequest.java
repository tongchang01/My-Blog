package com.tyb.myblog.v2.content.web;

import com.tyb.myblog.v2.content.application.tag.CreateTagCommand;

/**
 * 新增标签 HTTP 请求。
 */
public class CreateTagRequest extends TagWriteRequestSupport {

    public CreateTagCommand toCommand() {
        Values values = values();
        return new CreateTagCommand(
                values.nameZh(),
                values.nameJa(),
                values.nameEn(),
                values.slug());
    }
}
