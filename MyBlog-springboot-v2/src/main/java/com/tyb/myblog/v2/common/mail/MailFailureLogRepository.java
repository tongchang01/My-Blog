package com.tyb.myblog.v2.common.mail;

public interface MailFailureLogRepository {

    void insertFailed(MailFailureLog log);
}
