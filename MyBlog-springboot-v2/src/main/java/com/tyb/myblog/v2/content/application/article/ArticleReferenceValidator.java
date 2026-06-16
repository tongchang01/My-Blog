package com.tyb.myblog.v2.content.application.article;

import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.content.domain.article.ArticleStatus;
import com.tyb.myblog.v2.content.domain.category.Category;
import com.tyb.myblog.v2.content.domain.category.CategoryRepository;
import com.tyb.myblog.v2.content.domain.tag.Tag;
import com.tyb.myblog.v2.content.domain.tag.TagRepository;
import com.tyb.myblog.v2.system.application.attachment.AttachmentReferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 文章写入前锁定并校验分类、标签和封面附件引用。
 */
@Component
@RequiredArgsConstructor
public class ArticleReferenceValidator {

    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final AttachmentReferenceService attachmentService;

    public void lockAndValidate(
            ArticleStatus status,
            Long categoryId,
            List<Long> tagIds,
            Long coverAttachmentId) {
        if (categoryId != null) {
            requireExactCategories(List.of(categoryId));
        } else if (status != ArticleStatus.DRAFT) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    "当前文章状态必须选择分类");
        }
        requireExactTags(normalizeTagIds(tagIds));
        if (coverAttachmentId != null) {
            attachmentService.requireActiveImageForUpdate(
                    coverAttachmentId);
        }
    }

    private void requireExactCategories(List<Long> ids) {
        List<Long> lockedIds = categoryRepository
                .findActiveByIdsForUpdate(ids)
                .stream()
                .map(Category::id)
                .toList();
        if (!lockedIds.equals(ids)) {
            throw new ApiException(
                    ApiErrorCode.NOT_FOUND,
                    "文章分类不存在");
        }
    }

    private void requireExactTags(List<Long> ids) {
        if (ids.isEmpty()) {
            return;
        }
        List<Long> lockedIds = tagRepository
                .findActiveByIdsForUpdate(ids)
                .stream()
                .map(Tag::id)
                .toList();
        if (!lockedIds.equals(ids)) {
            throw new ApiException(
                    ApiErrorCode.NOT_FOUND,
                    "文章标签不存在");
        }
    }

    private List<Long> normalizeTagIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return ids.stream()
                .sorted()
                .toList();
    }
}
