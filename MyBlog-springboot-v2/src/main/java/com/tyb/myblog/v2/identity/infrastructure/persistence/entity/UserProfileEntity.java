package com.tyb.myblog.v2.identity.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tyb.myblog.v2.common.infrastructure.persistence.entity.AuditOnlyBase;
import lombok.Getter;
import lombok.Setter;

/**
 * 用户资料持久化实体，对应 {@code t_user_info}。
 */
@Getter
@Setter
@TableName("t_user_info")
public class UserProfileEntity extends AuditOnlyBase {

    /**
     * 用户 ID，同时作为资料表主键。
     */
    @TableId(value = "user_id", type = IdType.INPUT)
    private Long userId;

    /**
     * 前台展示昵称。
     */
    private String nickname;

    /**
     * 头像公开 URL。
     */
    private String avatarUrl;

    /**
     * 中文个人简介。
     */
    private String bioZh;

    /**
     * 日文个人简介。
     */
    private String bioJa;

    /**
     * 英文个人简介。
     */
    private String bioEn;

    /**
     * 公开所在地。
     */
    private String location;

    /**
     * 个人主页 URL。
     */
    private String website;

    /**
     * 公开联系邮箱。
     */
    private String emailPublic;

    /**
     * GitHub 主页 URL。
     */
    private String githubUrl;

    /**
     * Twitter 或 X 主页 URL。
     */
    private String twitterUrl;

    /**
     * LinkedIn 主页 URL。
     */
    private String linkedinUrl;

    /**
     * 知乎主页 URL。
     */
    private String zhihuUrl;

    /**
     * Qiita 主页 URL。
     */
    private String qiitaUrl;

    /**
     * 掘金主页 URL。
     */
    private String juejinUrl;
}
