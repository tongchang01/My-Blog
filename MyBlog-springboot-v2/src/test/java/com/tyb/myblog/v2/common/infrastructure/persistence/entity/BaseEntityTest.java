package com.tyb.myblog.v2.common.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class BaseEntityTest {

    @Test
    void baseEntityAddsAssignIdToAuditFields() throws NoSuchFieldException {
        assertThat(Modifier.isAbstract(BaseEntity.class.getModifiers())).isTrue();
        assertThat(BaseEntity.class.getSuperclass()).isEqualTo(AuditOnlyBase.class);

        Field id = BaseEntity.class.getDeclaredField("id");
        assertThat(id.getType()).isEqualTo(Long.class);
        assertThat(id.getAnnotation(TableId.class).type()).isEqualTo(IdType.ASSIGN_ID);
    }

    @Test
    void auditOnlyBaseDefinesSevenAuditFields() throws NoSuchFieldException {
        assertThat(Modifier.isAbstract(AuditOnlyBase.class.getModifiers())).isTrue();
        assertThat(AuditOnlyBase.class.getDeclaredFields())
                .extracting(Field::getName)
                .containsExactlyInAnyOrder(
                        "createdAt",
                        "createdBy",
                        "updatedAt",
                        "updatedBy",
                        "deleted",
                        "deletedAt",
                        "deletedBy");

        assertThat(field("createdAt").getType()).isEqualTo(LocalDateTime.class);
        assertThat(field("createdBy").getType()).isEqualTo(Long.class);
        assertThat(field("updatedAt").getType()).isEqualTo(LocalDateTime.class);
        assertThat(field("updatedBy").getType()).isEqualTo(Long.class);
        assertThat(field("deleted").getType()).isEqualTo(Integer.class);
        assertThat(field("deletedAt").getType()).isEqualTo(LocalDateTime.class);
        assertThat(field("deletedBy").getType()).isEqualTo(Long.class);
    }

    @Test
    void auditFieldsDeclareFillAndLogicalDeleteRules() throws NoSuchFieldException {
        assertThat(field("createdAt").getAnnotation(TableField.class).fill()).isEqualTo(FieldFill.INSERT);
        assertThat(field("createdBy").getAnnotation(TableField.class).fill()).isEqualTo(FieldFill.INSERT);
        assertThat(field("updatedAt").getAnnotation(TableField.class).fill()).isEqualTo(FieldFill.INSERT_UPDATE);
        assertThat(field("updatedBy").getAnnotation(TableField.class).fill()).isEqualTo(FieldFill.INSERT_UPDATE);

        TableLogic tableLogic = field("deleted").getAnnotation(TableLogic.class);
        assertThat(tableLogic.value()).isEqualTo("0");
        assertThat(tableLogic.delval()).isEqualTo("1");
        assertThat(new TestAuditEntity().getDeleted()).isZero();
    }

    private Field field(String name) throws NoSuchFieldException {
        return AuditOnlyBase.class.getDeclaredField(name);
    }

    private static final class TestAuditEntity extends AuditOnlyBase {
    }
}
