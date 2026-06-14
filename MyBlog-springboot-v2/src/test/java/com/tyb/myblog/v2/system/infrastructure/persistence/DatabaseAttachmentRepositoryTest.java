package com.tyb.myblog.v2.system.infrastructure.persistence;

import com.tyb.myblog.v2.common.storage.StorageType;
import com.tyb.myblog.v2.system.domain.attachment.Attachment;
import com.tyb.myblog.v2.system.domain.attachment.AttachmentLookup;
import com.tyb.myblog.v2.system.domain.attachment.AttachmentPage;
import com.tyb.myblog.v2.system.domain.attachment.AttachmentRepository;
import com.tyb.myblog.v2.system.domain.attachment.NewAttachment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
class DatabaseAttachmentRepositoryTest {

    private static final String ACTIVE_HASH = "a".repeat(64);
    private static final String DELETED_HASH = "b".repeat(64);

    @Autowired
    private AttachmentRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearAttachments() {
        jdbcTemplate.update("DELETE FROM t_attachment");
    }

    @Test
    void readsActiveByIdAndIgnoresDeleted() {
        insert(101L, ACTIVE_HASH, false, "2026-06-14 10:00:00");
        insert(102L, DELETED_HASH, true, "2026-06-14 11:00:00");

        assertThat(repository.findActiveById(101L)).isPresent();
        assertThat(repository.findActiveById(102L)).isEmpty();
    }

    @Test
    void findsDeletedRecordByHash() {
        insert(102L, DELETED_HASH, true, "2026-06-14 11:00:00");

        assertThat(repository.findByHashIncludingDeleted(DELETED_HASH))
                .get()
                .extracting(AttachmentLookup::deleted)
                .isEqualTo(true);
    }

    @Test
    void pagesActiveAttachmentsInStableNewestFirstOrder() {
        insert(101L, "1".repeat(64), false, "2026-06-14 10:00:00");
        insert(102L, "2".repeat(64), false, "2026-06-14 11:00:00");
        insert(103L, "3".repeat(64), false, "2026-06-14 11:00:00");
        insert(104L, "4".repeat(64), true, "2026-06-14 12:00:00");

        AttachmentPage page = repository.findActivePage(1, 2);

        assertThat(page.total()).isEqualTo(3);
        assertThat(page.records()).extracting(Attachment::id)
                .containsExactly(103L, 102L);
    }

    @Test
    void insertsWithAssignedIdAndExplicitCreator() {
        Attachment inserted = repository.insert(NewAttachment.create(
                StorageType.LOCAL,
                "local",
                "attachments/2026/06/new.png",
                "http://localhost:8080/media/attachments/2026/06/new.png",
                "image/png",
                128L,
                10,
                20,
                "new.png",
                "c".repeat(64),
                2001L));

        assertThat(inserted.id()).isPositive();
        assertThat(inserted.createdBy()).isEqualTo(2001L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT created_by FROM t_attachment WHERE id = ?",
                Long.class,
                inserted.id())).isEqualTo(2001L);
    }

    @Test
    void restoresDeletedOnlyOnceAndClearsDeleteAudit() {
        insert(102L, DELETED_HASH, true, "2026-06-14 11:00:00");
        LocalDateTime updatedAt = LocalDateTime.of(2026, 6, 14, 13, 0);

        assertThat(repository.restoreDeleted(102L, updatedAt, 3001L)).isTrue();
        assertThat(repository.restoreDeleted(102L, updatedAt, 3001L)).isFalse();
        assertThat(jdbcTemplate.queryForMap("""
                SELECT deleted, deleted_at, deleted_by, updated_by
                FROM t_attachment WHERE id = 102
                """))
                .containsEntry("DELETED", 0)
                .containsEntry("DELETED_AT", null)
                .containsEntry("DELETED_BY", null)
                .containsEntry("UPDATED_BY", 3001L);
    }

    private void insert(
            long id,
            String hash,
            boolean deleted,
            String createdAt) {
        jdbcTemplate.update("""
                INSERT INTO t_attachment (
                    id, storage_type, bucket, object_key, public_url,
                    content_type, file_size, width, height,
                    original_filename, hash_sha256,
                    created_at, created_by, updated_at, updated_by,
                    deleted, deleted_at, deleted_by
                ) VALUES (?, 'LOCAL', 'local', ?, ?, 'image/png',
                    128, 10, 20, 'cover.png', ?,
                    ?, 1001, ?, 1001, ?, ?, ?)
                """,
                id,
                "attachments/2026/06/" + id + ".png",
                "http://localhost/media/" + id + ".png",
                hash,
                createdAt,
                createdAt,
                deleted ? 1 : 0,
                deleted ? createdAt : null,
                deleted ? 1001L : null);
    }
}
