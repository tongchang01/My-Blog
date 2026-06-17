package com.tyb.myblog.v2.common.mail;

public record MailSendResult(
        boolean success,
        String providerMessageId,
        String errorMessage) {

    public static MailSendResult success(String providerMessageId) {
        return new MailSendResult(true, providerMessageId, null);
    }

    public static MailSendResult failed(String errorMessage) {
        return new MailSendResult(false, null, errorMessage);
    }
}
