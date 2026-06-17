package com.tyb.myblog.v2.common.mail;

public interface MailSender {

    MailSendResult send(MailSendCommand command);
}
