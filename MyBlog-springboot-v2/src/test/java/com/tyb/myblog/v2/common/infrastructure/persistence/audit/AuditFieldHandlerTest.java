package com.tyb.myblog.v2.common.infrastructure.persistence.audit;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.infrastructure.persistence.entity.BaseEntity;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuditFieldHandlerTest {

    private static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");
    private static final Instant FIXED_INSTANT = Instant.parse("2026-06-06T01:30:00Z");

    private final AuditFieldHandler handler = new AuditFieldHandler(
            Clock.fixed(FIXED_INSTANT, TOKYO),
            new SecurityContextAuditor());

    @BeforeAll
    static void initializeTableMetadata() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, TestEntity.class);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void fillsCreatedAndUpdatedFieldsOnInsert() {
        authenticate("1001");
        TestEntity entity = new TestEntity();
        MetaObject metaObject = SystemMetaObject.forObject(entity);

        handler.insertFill(metaObject);

        LocalDateTime expected = LocalDateTime.of(2026, 6, 6, 10, 30);
        assertThat(entity.getCreatedAt()).isEqualTo(expected);
        assertThat(entity.getCreatedBy()).isEqualTo(1001L);
        assertThat(entity.getUpdatedAt()).isEqualTo(expected);
        assertThat(entity.getUpdatedBy()).isEqualTo(1001L);
        assertThat(entity.getDeleted()).isZero();
        assertThat(entity.getDeletedAt()).isNull();
        assertThat(entity.getDeletedBy()).isNull();
    }

    @Test
    void fillsOnlyUpdatedFieldsOnUpdate() {
        authenticate("1002");
        TestEntity entity = new TestEntity();
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 9, 0);
        entity.setCreatedAt(createdAt);
        entity.setCreatedBy(99L);
        MetaObject metaObject = SystemMetaObject.forObject(entity);

        handler.updateFill(metaObject);

        assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
        assertThat(entity.getCreatedBy()).isEqualTo(99L);
        assertThat(entity.getUpdatedAt()).isEqualTo(LocalDateTime.of(2026, 6, 6, 10, 30));
        assertThat(entity.getUpdatedBy()).isEqualTo(1002L);
    }

    @Test
    void leavesUserAuditFieldsNullForSystemTask() {
        TestEntity entity = new TestEntity();
        MetaObject metaObject = SystemMetaObject.forObject(entity);

        handler.insertFill(metaObject);

        assertThat(entity.getCreatedBy()).isNull();
        assertThat(entity.getUpdatedBy()).isNull();
        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getUpdatedAt()).isNotNull();
    }

    private void authenticate(String userId) {
        AuthenticatedPrincipal principal = new AuthenticatedPrincipal(userId, "admin", List.of("ADMIN"));
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of()));
    }

    private static final class TestEntity extends BaseEntity {
    }
}
