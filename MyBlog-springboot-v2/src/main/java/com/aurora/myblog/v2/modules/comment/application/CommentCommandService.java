package com.aurora.myblog.v2.modules.comment.application;

import com.aurora.myblog.v2.modules.comment.domain.CommentCreateCommand;
import com.aurora.myblog.v2.modules.comment.domain.CommentType;
import com.aurora.myblog.v2.modules.comment.domain.CommentWriter;
import org.springframework.stereotype.Service;

@Service
public class CommentCommandService {

    private final CommentWriter commentWriter;

    public CommentCommandService(CommentWriter commentWriter) {
        this.commentWriter = commentWriter;
    }

    public CommentCreateResult createComment(String userId,
                                             Integer type,
                                             Integer topicId,
                                             Integer parentId,
                                             Integer replyUserId,
                                             String content,
                                             String clientIp,
                                             String userAgent) {
        int id = commentWriter.save(new CommentCreateCommand(
                Integer.parseInt(userId),
                CommentType.fromCode(type),
                topicId,
                parentId,
                replyUserId,
                content,
                clientIp,
                userAgent));
        return new CommentCreateResult(id, false);
    }

    public record CommentCreateResult(int id, boolean review) {
    }
}
