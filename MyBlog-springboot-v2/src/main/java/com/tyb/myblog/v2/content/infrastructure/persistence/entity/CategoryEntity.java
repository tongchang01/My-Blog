package com.tyb.myblog.v2.content.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tyb.myblog.v2.common.infrastructure.persistence.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * 分类持久化实体，对应 {@code t_category}。
 */
@Getter
@Setter
@TableName("t_category")
public class CategoryEntity extends BaseEntity {

    private String nameZh;

    private String nameJa;

    private String nameEn;

    private String slug;

    private Integer sortOrder;
}
