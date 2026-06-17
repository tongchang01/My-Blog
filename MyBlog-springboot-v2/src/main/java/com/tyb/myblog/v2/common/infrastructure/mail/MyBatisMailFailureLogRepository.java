package com.tyb.myblog.v2.common.infrastructure.mail;

import com.tyb.myblog.v2.common.mail.MailFailureLog;
import com.tyb.myblog.v2.common.mail.MailFailureLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MyBatisMailFailureLogRepository implements MailFailureLogRepository {

    private static final int FAILED_STATUS = 2;

    private final MailFailureLogMapper mapper;

    @Override
    public void insertFailed(MailFailureLog log) {
        MailFailureLogEntity entity = new MailFailureLogEntity();
        entity.setToEmail(log.toEmail());
        entity.setTemplate(log.template());
        entity.setSubject(log.subject());
        entity.setStatus(FAILED_STATUS);
        entity.setRetryCount(0);
        entity.setErrorMessage(truncate(mask(log.errorMessage()), 512));
        entity.setParamsJson(truncate(log.paramsJson(), 512));
        entity.setCreatedAt(log.createdAt());
        if (mapper.insertFailed(entity) != 1) {
            throw new IllegalStateException("邮件失败日志写入失败");
        }
    }

    private static String mask(String value) {
        if (value == null) {
            return null;
        }
        return value.replaceAll("(?i)Bearer\\s+[A-Za-z0-9._\\-]+", "Bearer ***")
                .replaceAll("(?i)api[_-]?key[=:][^\\s,;]+", "api_key=***");
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
