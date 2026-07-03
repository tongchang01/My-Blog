package com.tyb.myblog.v2.content.web;

import com.tyb.myblog.v2.content.application.category.CategoryResult;
import com.tyb.myblog.v2.content.application.category.PublicCategoryResult;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

/**
 * 分类应用结果到 HTTP 响应的机械映射。
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CategoryWebMapping {

    default PublicCategoryVO toPublicVO(PublicCategoryResult source) {
        return new PublicCategoryVO(
                Long.toString(source.id()),
                source.name(),
                source.slug(),
                source.articleCount());
    }

    default AdminCategoryVO toAdminVO(CategoryResult source) {
        return new AdminCategoryVO(
                Long.toString(source.id()),
                source.nameZh(),
                source.nameJa(),
                source.nameEn(),
                source.slug(),
                source.sortOrder(),
                source.createdAt(),
                nullableId(source.createdBy()),
                source.updatedAt(),
                nullableId(source.updatedBy()));
    }

    private static String nullableId(Long value) {
        return value == null ? null : Long.toString(value);
    }
}
