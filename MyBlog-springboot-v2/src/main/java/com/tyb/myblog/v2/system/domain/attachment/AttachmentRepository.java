package com.tyb.myblog.v2.system.domain.attachment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 附件持久化端口。
 */
public interface AttachmentRepository {

    Optional<Attachment> findActiveById(long id);

    Optional<Attachment> findActiveByIdForUpdate(long id);

    List<Attachment> findActiveByIds(List<Long> ids);

    Optional<AttachmentLookup> findByHashIncludingDeleted(String hashSha256);

    AttachmentPage findActivePage(int page, int size);

    AttachmentPage findDeletedPage(int page, int size);

    Attachment insert(NewAttachment attachment);

    boolean softDelete(long id, LocalDateTime deletedAt, long deletedBy);

    boolean restoreDeleted(long id, LocalDateTime updatedAt, long updatedBy);
}
