package com.tyb.myblog.v2.common.mail;

import java.util.Map;

public record MailSendCommand(
        String toEmail,
        String template,
        String subject,
        String bodyText,
        Map<String, String> params) {

    public MailSendCommand {
        params = params == null ? Map.of() : Map.copyOf(params);
    }
}
