package com.tyb.myblog.v2.common.mail;

import java.time.LocalDateTime;

public record MailFailureLog(
        String toEmail,
        String template,
        String subject,
        String errorMessage,
        String paramsJson,
        LocalDateTime createdAt) {
}
