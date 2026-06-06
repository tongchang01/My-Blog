package com.tyb.myblog.v2.common.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;

/**
 * 带雪花主键的标准业务实体基类。
 */
public abstract class BaseEntity extends AuditOnlyBase {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
