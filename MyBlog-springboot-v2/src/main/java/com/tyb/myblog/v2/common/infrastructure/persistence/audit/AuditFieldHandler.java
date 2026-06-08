package com.tyb.myblog.v2.common.infrastructure.persistence.audit;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * MyBatis-Plus 审计字段自动填充器。
 *
 * <p>只处理创建和更新时间、操作人。软删除时间和删除人必须由应用服务在执行软删除时显式设置。</p>
 */
public class AuditFieldHandler implements MetaObjectHandler {

    private final Clock clock;
    private final SecurityContextAuditor auditor;

    public AuditFieldHandler(Clock clock, SecurityContextAuditor auditor) {
        this.clock = clock;
        this.auditor = auditor;
    }

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now(clock);
        Long currentUserId = auditor.currentUserId();

        strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
        strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
        if (currentUserId != null) {
            strictInsertFill(metaObject, "createdBy", Long.class, currentUserId);
            strictInsertFill(metaObject, "updatedBy", Long.class, currentUserId);
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        Long currentUserId = auditor.currentUserId();

        if (metaObject.hasSetter("updatedAt")) {
            metaObject.setValue("updatedAt", LocalDateTime.now(clock));
        }
        if (currentUserId != null && metaObject.hasSetter("updatedBy")) {
            metaObject.setValue("updatedBy", currentUserId);
        }
    }
}
