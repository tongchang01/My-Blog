package com.tyb.myblog.v2.content.infrastructure.persistence.mapping;

import com.tyb.myblog.v2.content.domain.tag.NewTag;
import com.tyb.myblog.v2.content.domain.tag.Tag;
import com.tyb.myblog.v2.content.infrastructure.persistence.entity.TagEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

/**
 * 标签领域对象与持久化实体的机械映射。
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface TagPersistenceMapping {

    default Tag toDomain(TagEntity entity) {
        return Tag.reconstitute(
                entity.getId(),
                entity.getNameZh(),
                entity.getNameJa(),
                entity.getNameEn(),
                entity.getSlug(),
                entity.getCreatedAt(),
                entity.getCreatedBy(),
                entity.getUpdatedAt(),
                entity.getUpdatedBy());
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "nameZh", source = "name.zh")
    @Mapping(target = "nameJa", source = "name.ja")
    @Mapping(target = "nameEn", source = "name.en")
    @Mapping(target = "slug", source = "slug.value")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", source = "createdBy")
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    TagEntity toEntity(NewTag tag);

    @Mapping(target = "nameZh", source = "name.zh")
    @Mapping(target = "nameJa", source = "name.ja")
    @Mapping(target = "nameEn", source = "name.en")
    @Mapping(target = "slug", source = "slug.value")
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    TagEntity toEntity(Tag tag);
}
