package com.tyb.myblog.v2.content.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 旧库标签表实体。
 *
 * <p>对应数据库表 {@code t_tag}，当前主要服务前台标签导航、热门标签统计和后续后台标签管理。</p>
 */
@TableName("t_tag")
public class TagEntity {

    /**
     * 对应表字段 {@code id}，标签主键 ID。
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 对应表字段 {@code tag_name}，标签名称。
     */
    @TableField("tag_name")
    private String tagName;

    /**
     * 对应表字段 {@code create_time}，标签创建时间。
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 对应表字段 {@code update_time}，标签最后更新时间。
     */
    @TableField("update_time")
    private LocalDateTime updateTime;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
