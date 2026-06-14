package com.tyb.myblog.v2.system.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tyb.myblog.v2.common.infrastructure.persistence.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * 友链持久化实体，对应 {@code t_friend_link}。
 */
@Getter
@Setter
@TableName("t_friend_link")
public class FriendLinkEntity extends BaseEntity {

    private String name;

    private String url;

    private String avatarUrl;

    private String description;

    private Integer sortOrder;

    private Integer status;
}
