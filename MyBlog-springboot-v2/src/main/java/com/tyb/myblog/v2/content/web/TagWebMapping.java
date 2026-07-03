package com.tyb.myblog.v2.content.web;

import com.tyb.myblog.v2.content.application.tag.PublicTagResult;
import com.tyb.myblog.v2.content.application.tag.TagResult;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

/**
 * 标签应用结果到 HTTP 响应的机械映射。
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface TagWebMapping {

    default PublicTagVO toPublicVO(PublicTagResult source) {
        return new PublicTagVO(
                Long.toString(source.id()),
                source.name(),
                source.slug(),
                source.articleCount());
    }

    default AdminTagVO toAdminVO(TagResult source) {
        return new AdminTagVO(
                Long.toString(source.id()),
                source.nameZh(),
                source.nameJa(),
                source.nameEn(),
                source.slug(),
                source.createdAt(),
                nullableId(source.createdBy()),
                source.updatedAt(),
                nullableId(source.updatedBy()));
    }

    private static String nullableId(Long value) {
        return value == null ? null : Long.toString(value);
    }
}
