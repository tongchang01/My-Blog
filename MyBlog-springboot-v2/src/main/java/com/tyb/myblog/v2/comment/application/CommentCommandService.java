package com.tyb.myblog.v2.comment.application;

import com.tyb.myblog.v2.comment.domain.CommentCreateCommand;
import com.tyb.myblog.v2.comment.domain.CommentType;
import com.tyb.myblog.v2.comment.domain.CommentWriter;
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
