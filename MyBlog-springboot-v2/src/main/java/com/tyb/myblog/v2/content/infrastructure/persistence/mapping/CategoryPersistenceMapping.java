package com.tyb.myblog.v2.content.infrastructure.persistence.mapping;

import com.tyb.myblog.v2.content.domain.category.Category;
import com.tyb.myblog.v2.content.domain.category.NewCategory;
import com.tyb.myblog.v2.content.infrastructure.persistence.entity.CategoryEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

/**
 * 分类领域对象与持久化实体的机械映射。
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CategoryPersistenceMapping {

    default Category toDomain(CategoryEntity entity) {
        return Category.reconstitute(
                entity.getId(),
                entity.getNameZh(),
                entity.getNameJa(),
                entity.getNameEn(),
                entity.getSlug(),
                entity.getSortOrder(),
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
    CategoryEntity toEntity(NewCategory category);
}
