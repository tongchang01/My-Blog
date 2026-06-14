package com.tyb.myblog.v2.system.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.tyb.myblog.v2.common.infrastructure.persistence.entity.AuditOnlyBase;
import lombok.Getter;
import lombok.Setter;

/**
 * 站点配置持久化实体，对应固定单行表 {@code t_site_config}。
 */
@Getter
@Setter
@TableName("t_site_config")
public class SiteConfigEntity extends AuditOnlyBase {

    /** 固定主键，始终为 1。 */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /** 中文站点标题。 */
    private String siteTitleZh;

    /** 日文站点标题。 */
    private String siteTitleJa;

    /** 英文站点标题。 */
    private String siteTitleEn;

    /** 中文站点副标题。 */
    private String siteSubtitleZh;

    /** 日文站点副标题。 */
    private String siteSubtitleJa;

    /** 英文站点副标题。 */
    private String siteSubtitleEn;

    /** 中文关于我 Markdown。 */
    private String aboutMdZh;

    /** 日文关于我 Markdown。 */
    private String aboutMdJa;

    /** 英文关于我 Markdown。 */
    private String aboutMdEn;

    /** 站点 Logo URL。 */
    private String logoUrl;

    /** 站点 favicon URL。 */
    private String faviconUrl;

    /** ICP 备案号。 */
    private String icpNo;

    /** Spotify 播放列表 ID。 */
    private String spotifyPlaylistId;
}
