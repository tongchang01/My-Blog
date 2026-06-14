package com.tyb.myblog.v2.system.infrastructure.persistence;

import com.tyb.myblog.v2.system.domain.friendlink.FriendLink;
import com.tyb.myblog.v2.system.domain.friendlink.FriendLinkPage;
import com.tyb.myblog.v2.system.domain.friendlink.FriendLinkRepository;
import com.tyb.myblog.v2.system.domain.friendlink.FriendLinkStatus;
import com.tyb.myblog.v2.system.domain.friendlink.NewFriendLink;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@SpringBootTest
class DatabaseFriendLinkRepositoryTest {

    @Autowired
    private FriendLinkRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearFriendLinks() {
        jdbcTemplate.update("DELETE FROM t_friend_link");
    }

    @Test
    void readsOnlyVisibleActiveLinksForPublicViewInStableOrder() {
        insert(101L, 1, 20, false);
        insert(102L, 2, 0, false);
        insert(103L, 1, 10, false);
        insert(104L, 1, 0, true);

        assertThat(repository.findPublicVisible())
                .extracting(FriendLink::id)
                .containsExactly(103L, 101L);
    }

    @Test
    void pagesAllActiveStatusesInStableOrder() {
        insert(101L, 1, 20, false);
        insert(102L, 2, 0, false);
        insert(103L, 1, 0, false);

        FriendLinkPage page = repository.findActivePage(1, 2);

        assertThat(page.total()).isEqualTo(3);
        assertThat(page.records())
                .extracting(FriendLink::id)
                .containsExactly(102L, 103L);
        assertThat(page.page()).isEqualTo(1);
        assertThat(page.size()).isEqualTo(2);
    }

    @Test
    @Transactional
    void readsOnlyActiveRowsByIdentityAndLocksInStableOrder() {
        insert(101L, 1, 20, false);
        insert(102L, 2, 0, true);
        insert(103L, 1, 10, false);

        assertThat(repository.findActiveById(101L)).isPresent();
        assertThat(repository.findActiveById(102L)).isEmpty();
        assertThat(repository.findActiveByIdForUpdate(101L)).isPresent();
        assertThat(repository.findActiveByIdsForUpdate(
                List.of(103L, 101L)))
                .extracting(FriendLink::id)
                .containsExactly(101L, 103L);
    }

    @Test
    void insertsWithAssignedIdAndExplicitAuditUsers() {
        FriendLink inserted = repository.insert(NewFriendLink.create(
                "Example",
                "https://example.com",
                null,
                "介绍",
                10,
                FriendLinkStatus.VISIBLE,
                2001L));

        assertThat(inserted.id()).isPositive();
        assertThat(inserted.createdBy()).isEqualTo(2001L);
        assertThat(inserted.updatedBy()).isEqualTo(2001L);
        assertThat(jdbcTemplate.queryForMap("""
                SELECT created_by, updated_by
                FROM t_friend_link WHERE id = ?
                """, inserted.id()))
                .containsEntry("CREATED_BY", 2001L)
                .containsEntry("UPDATED_BY", 2001L);
    }

    @Test
    void rejectsUnknownDatabaseStatus() {
        insert(101L, 9, 0, false);

        assertThatThrownBy(() -> repository.findActiveById(101L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updatesAllBusinessFieldsAndAudit() {
        insert(101L, 1, 0, false);
        FriendLink current = repository.findActiveById(101L)
                .orElseThrow();
        FriendLink updated = current.replace(
                "Updated",
                "https://updated.example.com",
                null,
                null,
                30,
                FriendLinkStatus.HIDDEN);
        LocalDateTime now =
                LocalDateTime.of(2026, 6, 14, 13, 0);

        assertThat(repository.update(
                updated, now, 2001L)).isTrue();

        assertThat(jdbcTemplate.queryForMap("""
                SELECT name, url, avatar_url, description,
                       sort_order, status, updated_by
                FROM t_friend_link WHERE id = 101
                """))
                .containsEntry("NAME", "Updated")
                .containsEntry("URL", "https://updated.example.com")
                .containsEntry("AVATAR_URL", null)
                .containsEntry("DESCRIPTION", null)
                .containsEntry("SORT_ORDER", 30)
                .containsEntry("STATUS", 2)
                .containsEntry("UPDATED_BY", 2001L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT updated_at FROM t_friend_link WHERE id = 101",
                LocalDateTime.class))
                .isEqualTo(now);
    }

    private void insert(
            long id,
            int status,
            int sortOrder,
            boolean deleted) {
        jdbcTemplate.update("""
                INSERT INTO t_friend_link (
                    id, name, url, avatar_url, description,
                    sort_order, status,
                    created_at, created_by, updated_at, updated_by,
                    deleted, deleted_at, deleted_by
                ) VALUES (?, ?, ?, NULL, NULL, ?, ?,
                    '2026-06-14 12:00:00', 1001,
                    '2026-06-14 12:00:00', 1001,
                    ?, ?, ?)
                """,
                id,
                "Friend " + id,
                "https://example.com/" + id,
                sortOrder,
                status,
                deleted ? 1 : 0,
                deleted ? "2026-06-14 13:00:00" : null,
                deleted ? 1001L : null);
    }
}
