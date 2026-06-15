package com.tyb.myblog.v2.content.application.category;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.content.application.ContentAuthorization;
import com.tyb.myblog.v2.content.domain.category.Category;
import com.tyb.myblog.v2.content.domain.category.CategoryRepository;
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
 * 分类批量排序服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategorySortService {

    private static final int MAX_ITEMS = 100;
    private static final int MAX_SORT_ORDER = 1_000_000;

    private final CategoryRepository repository;
    private final ContentAuthorization authorization;
    private final Clock clock;

    /**
     * 按 ID 升序锁定全部目标，任一目标缺失或更新失败时整体回滚。
     */
    @Transactional
    public void update(
            AuthenticatedPrincipal principal,
            UpdateCategorySortOrdersCommand command) {
        long actorId = authorization.requireAdmin(principal);
        List<CategorySortItem> items = validate(command);
        List<Long> sortedIds = items.stream()
                .map(CategorySortItem::id)
                .sorted()
                .toList();
        List<Category> locked =
                repository.findActiveByIdsForUpdate(sortedIds);
        Set<Long> lockedIds = locked.stream()
                .map(Category::id)
                .collect(Collectors.toSet());
        if (lockedIds.size() != sortedIds.size()
                || !lockedIds.containsAll(sortedIds)) {
            throw new ApiException(
                    ApiErrorCode.NOT_FOUND,
                    "部分分类不存在");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        for (CategorySortItem item : items) {
            if (!repository.updateSortOrder(
                    item.id(),
                    item.sortOrder(),
                    now,
                    actorId)) {
                log.error(
                        "分类排序更新行数异常，categoryId={}",
                        item.id());
                throw new ApiException(ApiErrorCode.INTERNAL_ERROR);
            }
        }
    }

    private List<CategorySortItem> validate(
            UpdateCategorySortOrdersCommand command) {
        if (command == null
                || command.items() == null
                || command.items().isEmpty()
                || command.items().size() > MAX_ITEMS) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    "分类排序项数量必须在 1 到 100 之间");
        }
        List<CategorySortItem> items =
                List.copyOf(command.items());
        Set<Long> ids = new HashSet<>();
        for (CategorySortItem item : items) {
            if (item == null
                    || item.id() <= 0
                    || item.sortOrder() < 0
                    || item.sortOrder() > MAX_SORT_ORDER) {
                throw new ApiException(
                        ApiErrorCode.VALIDATION_ERROR,
                        "分类排序项不合法");
            }
            if (!ids.add(item.id())) {
                throw new ApiException(
                        ApiErrorCode.VALIDATION_ERROR,
                        "分类排序 ID 不能重复");
            }
        }
        return items;
    }
}
