package com.tyb.myblog.v2.system.application.attachment;

import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.system.domain.attachment.Attachment;
import com.tyb.myblog.v2.system.domain.attachment.AttachmentLookup;
import com.tyb.myblog.v2.system.domain.attachment.AttachmentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * 在短事务中条件恢复软删除附件。
 */
@Service
@RequiredArgsConstructor
public class AttachmentRestoreService {

    private static final Logger log =
            LoggerFactory.getLogger(AttachmentRestoreService.class);

    private final AttachmentRepository repository;
    private final Clock clock;

    @Transactional
    public Attachment restore(
            AttachmentLookup lookup,
            long actorId) {
        repository.restoreDeleted(
                lookup.attachment().id(),
                LocalDateTime.now(clock),
                actorId);
        return repository.findByHashIncludingDeleted(
                        lookup.attachment().hashSha256())
                .filter(result -> !result.deleted())
                .map(AttachmentLookup::attachment)
                .orElseThrow(() -> {
                    log.error(
                            "附件恢复后仍不可见，attachmentId={}",
                            lookup.attachment().id());
                    return new ApiException(
                            ApiErrorCode.INTERNAL_ERROR);
                });
    }
}
