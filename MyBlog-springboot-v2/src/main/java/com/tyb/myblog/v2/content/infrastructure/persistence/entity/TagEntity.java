package com.tyb.myblog.v2.content.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tyb.myblog.v2.common.infrastructure.persistence.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * 标签持久化实体，对应 {@code t_tag}。
 */
@Getter
@Setter
@TableName("t_tag")
public class TagEntity extends BaseEntity {

    private String nameZh;

    private String nameJa;

    private String nameEn;

    private String slug;
}
