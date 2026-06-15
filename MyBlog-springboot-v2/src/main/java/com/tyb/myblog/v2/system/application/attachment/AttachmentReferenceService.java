package com.tyb.myblog.v2.system.application.attachment;

import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.system.domain.attachment.Attachment;
import com.tyb.myblog.v2.system.domain.attachment.AttachmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 为其它业务模块提供附件引用校验和公开地址批量解析。
 */
@Service
@RequiredArgsConstructor
public class AttachmentReferenceService {

    private final AttachmentRepository repository;

    /**
     * 写入结构化附件引用前锁定并校验 active 图片。
     */
    public AttachmentReferenceResult requireActiveImageForUpdate(long id) {
        if (id <= 0) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    "封面附件 ID 必须为正数");
        }
        Attachment attachment = repository.findActiveByIdForUpdate(id)
                .orElseThrow(() -> new ApiException(
                        ApiErrorCode.NOT_FOUND,
                        "封面附件不存在"));
        if (!attachment.contentType().startsWith("image/")) {
            throw new ApiException(
                    ApiErrorCode.CONFLICT,
                    "封面附件必须是图片");
        }
        return new AttachmentReferenceResult(
                attachment.id(),
                attachment.publicUrl(),
                attachment.contentType());
    }

    /**
     * 批量解析 active 附件公开地址，缺失或已删除附件不进入结果。
     */
    public Map<Long, String> resolvePublicUrls(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        List<Long> normalized = ids.stream()
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .distinct()
                .sorted()
                .toList();
        if (normalized.isEmpty()) {
            return Map.of();
        }
        return repository.findActiveByIds(normalized).stream()
                .collect(Collectors.toUnmodifiableMap(
                        Attachment::id,
                        Attachment::publicUrl,
                        (left, right) -> left));
    }
}
