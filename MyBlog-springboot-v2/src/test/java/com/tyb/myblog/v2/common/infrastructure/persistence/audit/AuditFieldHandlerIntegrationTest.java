package com.tyb.myblog.v2.common.infrastructure.persistence.audit;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.infrastructure.persistence.entity.BaseEntity;
import org.apache.ibatis.annotations.Mapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@Sql(statements = {
        "drop table if exists t_audit_update_test",
        """
                create table t_audit_update_test (
                    id bigint not null,
                    display_name varchar(64) not null,
                    created_at datetime not null,
                    created_by bigint null,
                    updated_at datetime not null,
                    updated_by bigint null,
                    deleted tinyint not null default 0,
                    deleted_at datetime null,
                    deleted_by bigint null,
                    primary key (id)
                );
                """
})
class AuditFieldHandlerIntegrationTest {

    @Autowired
    private AuditUpdateTestMapper mapper;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void updateByIdOverwritesExistingAuditValues() {
        authenticate("1001");
        AuditUpdateTestEntity entity = new AuditUpdateTestEntity();
        entity.setName("before");
        mapper.insert(entity);

        LocalDateTime oldUpdatedAt = LocalDateTime.of(2020, 1, 1, 0, 0);
        entity.setName("after");
        entity.setUpdatedAt(oldUpdatedAt);
        entity.setUpdatedBy(99L);
        authenticate("1002");

        mapper.updateById(entity);

        AuditUpdateTestEntity updated = mapper.selectById(entity.getId());
        assertThat(updated.getName()).isEqualTo("after");
        assertThat(updated.getUpdatedAt()).isAfter(oldUpdatedAt);
        assertThat(updated.getUpdatedBy()).isEqualTo(1002L);
    }

    private void authenticate(String userId) {
        AuthenticatedPrincipal principal = new AuthenticatedPrincipal(userId, "admin", List.of("ADMIN"));
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of()));
    }
}

@Mapper
interface AuditUpdateTestMapper extends BaseMapper<AuditUpdateTestEntity> {
}

@TableName("t_audit_update_test")
class AuditUpdateTestEntity extends BaseEntity {

    @TableField("display_name")
    private String name;

    String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }
}
