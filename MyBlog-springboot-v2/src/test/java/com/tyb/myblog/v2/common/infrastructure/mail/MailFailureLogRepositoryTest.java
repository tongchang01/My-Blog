package com.tyb.myblog.v2.common.infrastructure.mail;

import com.tyb.myblog.v2.common.mail.MailFailureLog;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MailFailureLogRepositoryTest {

    @Test
    void masksAndTruncatesFailureMessageBeforeInsert() {
        MailFailureLogMapper mapper = mock(MailFailureLogMapper.class);
        when(mapper.insertFailed(org.mockito.ArgumentMatchers.any()))
                .thenReturn(1);
        MyBatisMailFailureLogRepository repository =
                new MyBatisMailFailureLogRepository(mapper);

        repository.insertFailed(new MailFailureLog(
                "to@example.com",
                "comment_reply",
                "subject",
                "Bearer secret api_key=123 " + "x".repeat(600),
                "{}",
                LocalDateTime.of(2026, 6, 17, 20, 0)));

        verify(mapper).insertFailed(argThat(entity -> {
            assertThat(entity.getErrorMessage()).doesNotContain("secret");
            assertThat(entity.getErrorMessage()).doesNotContain("123");
            assertThat(entity.getErrorMessage()).hasSizeLessThanOrEqualTo(512);
            return true;
        }));
    }
}
