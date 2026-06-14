package com.tyb.myblog.v2.system.application.attachment;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.common.storage.StorageService;
import com.tyb.myblog.v2.common.storage.StorageServiceRegistry;
import com.tyb.myblog.v2.common.storage.StoredObject;
import com.tyb.myblog.v2.common.storage.config.StorageProperties;
import com.tyb.myblog.v2.common.storage.image.ImageInspector;
import com.tyb.myblog.v2.common.storage.image.InspectedImage;
import com.tyb.myblog.v2.common.storage.image.SpooledUpload;
import com.tyb.myblog.v2.common.storage.image.UploadSpooler;
import com.tyb.myblog.v2.system.domain.attachment.Attachment;
import com.tyb.myblog.v2.system.domain.attachment.AttachmentLookup;
import com.tyb.myblog.v2.system.domain.attachment.AttachmentRepository;
import com.tyb.myblog.v2.system.domain.attachment.NewAttachment;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * 编排附件临时落盘、识别、去重、存储和数据库登记。
 *
 * <p>该服务故意不加事务，物理存储写入后只调用独立短事务服务；数据库失败时可以在
 * 事务回滚之后补偿删除本次随机对象。</p>
 */
@Service
@RequiredArgsConstructor
public class AttachmentUploadService {

    private static final Logger log =
            LoggerFactory.getLogger(AttachmentUploadService.class);

    private final AttachmentRepository repository;
    private final StorageServiceRegistry registry;
    private final UploadSpooler spooler;
    private final ImageInspector inspector;
    private final AttachmentRegistrationService registrationService;
    private final AttachmentRestoreService restoreService;
    private final ObjectKeyGenerator keyGenerator;
    private final StorageProperties properties;

    public AttachmentResult upload(
            AuthenticatedPrincipal principal,
            AttachmentUploadCommand command) {
        long actorId = requireAdmin(principal);
        if (command == null || command.inputStream() == null) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    "上传文件不能为空");
        }
        try (InputStream input = command.inputStream();
             SpooledUpload upload = spooler.spool(
                     input, properties.getMaxFileBytes())) {
            InspectedImage image = inspector.inspect(upload.path());
            Optional<AttachmentLookup> duplicate =
                    repository.findByHashIncludingDeleted(upload.sha256());
            Attachment attachment = duplicate.isPresent()
                    ? reuseOrRestore(duplicate.get(), actorId)
                    : storeAndRegister(
                            upload,
                            image,
                            command.originalFilename(),
                            actorId);
            return AttachmentResult.from(attachment);
        } catch (ApiException exception) {
            throw exception;
        } catch (IllegalArgumentException exception) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    exception.getMessage());
        } catch (IOException exception) {
            log.error("附件临时文件处理失败", exception);
            throw internal(exception);
        } catch (RuntimeException exception) {
            log.error("附件上传失败", exception);
            throw internal(exception);
        }
    }

    private Attachment reuseOrRestore(
            AttachmentLookup lookup,
            long actorId) {
        Attachment attachment = lookup.attachment();
        StorageService storage =
                registry.required(attachment.storageType());
        if (!storage.exists(
                attachment.bucket(), attachment.objectKey())) {
            log.error(
                    "附件物理对象缺失，attachmentId={}, storageType={}, bucket={}, objectKey={}",
                    attachment.id(),
                    attachment.storageType(),
                    attachment.bucket(),
                    attachment.objectKey());
            throw new ApiException(ApiErrorCode.INTERNAL_ERROR);
        }
        return lookup.deleted()
                ? restoreService.restore(lookup, actorId)
                : attachment;
    }

    private Attachment storeAndRegister(
            SpooledUpload upload,
            InspectedImage image,
            String originalFilename,
            long actorId) {
        StorageService storage = registry.current();
        String objectKey = keyGenerator.generate(image.format());
        StoredObject object = storage.store(
                new com.tyb.myblog.v2.common.storage.StoreObjectCommand(
                        upload.path(),
                        objectKey,
                        image.format().contentType(),
                        upload.size()));
        NewAttachment attachment = NewAttachment.create(
                object.storageType(),
                object.bucket(),
                object.objectKey(),
                object.publicUrl(),
                image.format().contentType(),
                upload.size(),
                image.width(),
                image.height(),
                originalFilename,
                upload.sha256(),
                actorId);
        try {
            return registrationService.register(attachment);
        } catch (DuplicateKeyException exception) {
            compensate(storage, object, exception);
            AttachmentLookup winner =
                    repository.findByHashIncludingDeleted(upload.sha256())
                            .orElseThrow(() -> internal(exception));
            return reuseOrRestore(winner, actorId);
        } catch (RuntimeException exception) {
            compensate(storage, object, exception);
            throw exception;
        }
    }

    private void compensate(
            StorageService storage,
            StoredObject object,
            RuntimeException original) {
        try {
            storage.delete(object.bucket(), object.objectKey());
        } catch (RuntimeException compensationFailure) {
            original.addSuppressed(compensationFailure);
            log.error(
                    "附件对象补偿删除失败，storageType={}, bucket={}, objectKey={}",
                    object.storageType(),
                    object.bucket(),
                    object.objectKey(),
                    compensationFailure);
        }
    }

    private long requireAdmin(AuthenticatedPrincipal principal) {
        if (principal == null) {
            throw new ApiException(ApiErrorCode.INVALID_TOKEN);
        }
        if (!principal.roles().contains("ADMIN")) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
        try {
            long id = Long.parseLong(principal.id());
            if (id <= 0) {
                throw new NumberFormatException("non-positive");
            }
            return id;
        } catch (NumberFormatException exception) {
            throw new ApiException(ApiErrorCode.INVALID_TOKEN);
        }
    }

    private ApiException internal(Throwable cause) {
        ApiException exception =
                new ApiException(ApiErrorCode.INTERNAL_ERROR);
        exception.initCause(cause);
        return exception;
    }
}
