package com.tyb.myblog.v2.common.infrastructure.mail;

import com.tyb.myblog.v2.common.mail.MailSendCommand;
import com.tyb.myblog.v2.common.mail.MailSendResult;
import com.tyb.myblog.v2.common.mail.MailSender;

public class NoopMailSender implements MailSender {

    @Override
    public MailSendResult send(MailSendCommand command) {
        return MailSendResult.success(null);
    }
}
