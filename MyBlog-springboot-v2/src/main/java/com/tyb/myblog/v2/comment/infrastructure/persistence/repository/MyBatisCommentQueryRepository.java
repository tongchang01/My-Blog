package com.tyb.myblog.v2.comment.infrastructure.persistence.repository;

import com.tyb.myblog.v2.comment.domain.CommentPage;
import com.tyb.myblog.v2.comment.domain.CommentPageItem;
import com.tyb.myblog.v2.comment.domain.CommentQueryRepository;
import com.tyb.myblog.v2.comment.domain.CommentTarget;
import com.tyb.myblog.v2.comment.infrastructure.persistence.mapper.CommentMapper;
import com.tyb.myblog.v2.comment.infrastructure.persistence.projection.CommentPageRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class MyBatisCommentQueryRepository implements CommentQueryRepository {

    private final CommentMapper mapper;

    @Override
    public CommentPage page(CommentTarget target, int page, int size) {
        long offset = (long) (page - 1) * size;
        int targetType = target.targetType().databaseValue();
        List<CommentPageRow> roots = mapper.selectPublicRoots(
                targetType,
                target.targetId(),
                offset,
                size);
        List<CommentPageItem> records = attachReplies(roots);
        long total = mapper.countPublicRoots(targetType, target.targetId());
        return new CommentPage(records, total, page, size);
    }

    private List<CommentPageItem> attachReplies(List<CommentPageRow> roots) {
        if (roots.isEmpty()) {
            return List.of();
        }
        List<Long> rootIds = roots.stream()
                .map(CommentPageRow::id)
                .toList();
        Map<Long, List<CommentPageItem>> repliesByParent = mapper.selectPublicReplies(rootIds)
                .stream()
                .map(row -> toItem(row, List.of()))
                .collect(Collectors.groupingBy(CommentPageItem::parentId));
        return roots.stream()
                .map(row -> toItem(
                        row,
                        repliesByParent.getOrDefault(row.id(), List.of())))
                .toList();
    }

    private static CommentPageItem toItem(
            CommentPageRow row,
            List<CommentPageItem> replies) {
        return new CommentPageItem(
                row.id(),
                row.parentId(),
                row.replyToCommentId(),
                row.replyToNickname(),
                row.authorNickname(),
                row.authorSite(),
                row.contentHtml(),
                row.createdAt(),
                replies);
    }
}
