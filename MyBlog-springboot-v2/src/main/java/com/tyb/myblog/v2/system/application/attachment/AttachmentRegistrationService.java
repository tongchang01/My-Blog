package com.tyb.myblog.v2.system.application.attachment;

import com.tyb.myblog.v2.system.domain.attachment.Attachment;
import com.tyb.myblog.v2.system.domain.attachment.AttachmentRepository;
import com.tyb.myblog.v2.system.domain.attachment.NewAttachment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 在独立短事务中登记新附件元数据。
 */
@Service
@RequiredArgsConstructor
public class AttachmentRegistrationService {

    private final AttachmentRepository repository;

    @Transactional
    public Attachment register(NewAttachment attachment) {
        return repository.insert(attachment);
    }
}
