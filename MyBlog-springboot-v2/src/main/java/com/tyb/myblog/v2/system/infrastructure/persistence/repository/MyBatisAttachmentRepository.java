package com.tyb.myblog.v2.system.infrastructure.persistence.repository;

import com.tyb.myblog.v2.common.storage.StorageType;
import com.tyb.myblog.v2.system.domain.attachment.Attachment;
import com.tyb.myblog.v2.system.domain.attachment.AttachmentLookup;
import com.tyb.myblog.v2.system.domain.attachment.AttachmentPage;
import com.tyb.myblog.v2.system.domain.attachment.AttachmentRepository;
import com.tyb.myblog.v2.system.domain.attachment.NewAttachment;
import com.tyb.myblog.v2.system.infrastructure.persistence.entity.AttachmentEntity;
import com.tyb.myblog.v2.system.infrastructure.persistence.mapper.AttachmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 基于 MyBatis 的附件仓储适配器。
 */
@Repository
@RequiredArgsConstructor
public class MyBatisAttachmentRepository implements AttachmentRepository {

    private final AttachmentMapper mapper;

    @Override
    public Optional<Attachment> findActiveById(long id) {
        return Optional.ofNullable(mapper.selectActiveById(id))
                .map(this::toDomain);
    }

    @Override
    public Optional<Attachment> findActiveByIdForUpdate(long id) {
        return Optional.ofNullable(mapper.selectActiveByIdForUpdate(id))
                .map(this::toDomain);
    }

    @Override
    public List<Attachment> findActiveByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return mapper.selectActiveByIds(ids).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<AttachmentLookup> findByHashIncludingDeleted(
            String hashSha256) {
        return Optional.ofNullable(
                        mapper.selectByHashIncludingDeleted(hashSha256))
                .map(entity -> new AttachmentLookup(
                        toDomain(entity),
                        Integer.valueOf(1).equals(entity.getDeleted())));
    }

    @Override
    public AttachmentPage findActivePage(int page, int size) {
        long offset = Math.multiplyExact((long) page - 1L, size);
        return new AttachmentPage(
                mapper.selectActivePage(offset, size).stream()
                        .map(this::toDomain)
                        .toList(),
                mapper.countActive(),
                page,
                size);
    }

    @Override
    public AttachmentPage findDeletedPage(int page, int size) {
        long offset = Math.multiplyExact((long) page - 1L, size);
        return new AttachmentPage(
                mapper.selectDeletedPage(offset, size).stream()
                        .map(this::toDomain)
                        .toList(),
                mapper.countDeleted(),
                page,
                size);
    }

    @Override
    public Attachment insert(NewAttachment attachment) {
        AttachmentEntity entity = toEntity(attachment);
        if (mapper.insert(entity) != 1
                || entity.getId() == null
                || entity.getId() <= 0) {
            throw new IllegalStateException("附件登记失败");
        }
        return toDomain(entity);
    }

    @Override
    public boolean restoreDeleted(
            long id,
            LocalDateTime updatedAt,
            long updatedBy) {
        return mapper.restoreDeleted(id, updatedAt, updatedBy) == 1;
    }

    @Override
    public boolean softDelete(
            long id,
            LocalDateTime deletedAt,
            long deletedBy) {
        return mapper.softDelete(id, deletedAt, deletedBy) == 1;
    }

    private Attachment toDomain(AttachmentEntity entity) {
        return Attachment.reconstitute(
                entity.getId(),
                StorageType.parse(entity.getStorageType()),
                entity.getBucket(),
                entity.getObjectKey(),
                entity.getPublicUrl(),
                entity.getContentType(),
                entity.getFileSize(),
                entity.getWidth(),
                entity.getHeight(),
                entity.getOriginalFilename(),
                entity.getHashSha256(),
                entity.getCreatedAt(),
                entity.getCreatedBy());
    }

    private AttachmentEntity toEntity(NewAttachment attachment) {
        AttachmentEntity entity = new AttachmentEntity();
        entity.setStorageType(attachment.storageType().name());
        entity.setBucket(attachment.bucket());
        entity.setObjectKey(attachment.objectKey());
        entity.setPublicUrl(attachment.publicUrl());
        entity.setContentType(attachment.contentType());
        entity.setFileSize(attachment.fileSize());
        entity.setWidth(attachment.width());
        entity.setHeight(attachment.height());
        entity.setOriginalFilename(attachment.originalFilename());
        entity.setHashSha256(attachment.hashSha256());
        entity.setCreatedBy(attachment.createdBy());
        entity.setUpdatedBy(attachment.createdBy());
        return entity;
    }
}
