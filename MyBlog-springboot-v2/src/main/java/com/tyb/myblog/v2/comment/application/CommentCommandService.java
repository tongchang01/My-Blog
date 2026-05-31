package com.tyb.myblog.v2.comment.application;

import com.tyb.myblog.v2.comment.domain.CommentCreateCommand;
import com.tyb.myblog.v2.comment.domain.CommentType;
import com.tyb.myblog.v2.comment.domain.CommentWriter;
import org.springframework.stereotype.Service;

@Service
/**
 * 前台评论命令应用服务。
 *
 * <p>负责组装评论提交命令，并把当前登录用户、客户端 IP 和 User-Agent 一并传给写入端口。</p>
 */
public class CommentCommandService {

    private final CommentWriter commentWriter;

    public CommentCommandService(CommentWriter commentWriter) {
        this.commentWriter = commentWriter;
    }

    /**
     * 创建评论或回复。
     *
     * <p>当前返回 {@code review=false}，表示提交后不立即告诉前端进入审核状态；
     * 后续如果启用评论审核策略，需要在这里统一调整返回语义。</p>
     */
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

    /**
     * 评论创建结果。
     *
     * @param id     新评论 ID
     * @param review 是否进入审核中状态
     */
    public record CommentCreateResult(int id, boolean review) {
    }
}
