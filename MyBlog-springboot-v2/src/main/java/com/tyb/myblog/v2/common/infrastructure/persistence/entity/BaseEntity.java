package com.tyb.myblog.v2.common.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Getter;
import lombok.Setter;

/**
 * 带雪花主键的标准业务实体基类。
 */
@Getter
@Setter
public abstract class BaseEntity extends AuditOnlyBase {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
}
