package com.tyb.myblog.v2.system.application.friendlink;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.system.domain.friendlink.FriendLink;
import com.tyb.myblog.v2.system.domain.friendlink.FriendLinkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 友链批量排序服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FriendLinkSortService {

    private static final int MAX_ITEMS = 100;
    private static final int MAX_SORT_ORDER = 1_000_000;

    private final FriendLinkRepository repository;
    private final FriendLinkAuthorization authorization;
    private final Clock clock;

    /**
     * 按 ID 升序锁定全部目标，任一目标缺失或更新失败时整体回滚。
     */
    @Transactional
    public List<FriendLinkResult> update(
            AuthenticatedPrincipal principal,
            UpdateFriendLinkSortOrdersCommand command) {
        long actorId = authorization.requireAdmin(principal);
        List<FriendLinkSortItem> items = validate(command);
        List<Long> sortedIds = items.stream()
                .map(FriendLinkSortItem::id)
                .sorted()
                .toList();
        List<FriendLink> locked =
                repository.findActiveByIdsForUpdate(sortedIds);
        Set<Long> lockedIds = locked.stream()
                .map(FriendLink::id)
                .collect(Collectors.toSet());
        if (lockedIds.size() != sortedIds.size()
                || !lockedIds.containsAll(sortedIds)) {
            throw new ApiException(
                    ApiErrorCode.NOT_FOUND,
                    "部分友链不存在");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        for (FriendLinkSortItem item : items) {
            if (!repository.updateSortOrder(
                    item.id(), item.sortOrder(), now, actorId)) {
                log.error(
                        "友链排序更新行数异常，friendLinkId={}",
                        item.id());
                throw new ApiException(ApiErrorCode.INTERNAL_ERROR);
            }
        }
        return items.stream()
                .map(item -> repository.findActiveById(item.id())
                        .map(FriendLinkResult::from)
                        .orElseThrow(this::missingAfterUpdate))
                .toList();
    }

    private List<FriendLinkSortItem> validate(
            UpdateFriendLinkSortOrdersCommand command) {
        if (command == null
                || command.items() == null
                || command.items().isEmpty()
                || command.items().size() > MAX_ITEMS) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    "友链排序项数量必须在 1 到 100 之间");
        }
        List<FriendLinkSortItem> items =
                List.copyOf(command.items());
        Set<Long> ids = new HashSet<>();
        for (FriendLinkSortItem item : items) {
            if (item == null
                    || item.id() <= 0
                    || item.sortOrder() < 0
                    || item.sortOrder() > MAX_SORT_ORDER) {
                throw new ApiException(
                        ApiErrorCode.VALIDATION_ERROR,
                        "友链排序项不合法");
            }
            if (!ids.add(item.id())) {
                throw new ApiException(
                        ApiErrorCode.VALIDATION_ERROR,
                        "友链排序 ID 不能重复");
            }
        }
        return items;
    }

    private ApiException missingAfterUpdate() {
        log.error("友链排序更新后无法重新读取");
        return new ApiException(ApiErrorCode.INTERNAL_ERROR);
    }
}
