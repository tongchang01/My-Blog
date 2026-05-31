package com.tyb.myblog.v2.content.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 旧库分类表实体。
 *
 * <p>对应数据库表 {@code t_category}，当前主要服务前台分类导航、分类文章数量统计和后续后台分类管理。</p>
 */
@TableName("t_category")
public class CategoryEntity {

    /**
     * 对应表字段 {@code id}，分类主键 ID。
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 对应表字段 {@code category_name}，分类名称。
     */
    @TableField("category_name")
    private String categoryName;

    /**
     * 对应表字段 {@code create_time}，分类创建时间。
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 对应表字段 {@code update_time}，分类最后更新时间。
     */
    @TableField("update_time")
    private LocalDateTime updateTime;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
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
