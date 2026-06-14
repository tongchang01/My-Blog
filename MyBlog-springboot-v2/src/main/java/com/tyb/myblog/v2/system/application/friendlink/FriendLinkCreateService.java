package com.tyb.myblog.v2.system.application.friendlink;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.system.domain.friendlink.FriendLinkRepository;
import com.tyb.myblog.v2.system.domain.friendlink.NewFriendLink;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 后台友链新增服务。
 */
@Service
@RequiredArgsConstructor
public class FriendLinkCreateService {

    private final FriendLinkRepository repository;
    private final FriendLinkAuthorization authorization;

    @Transactional
    public FriendLinkResult create(
            AuthenticatedPrincipal principal,
            CreateFriendLinkCommand command) {
        long actorId = authorization.requireAdmin(principal);
        if (command == null) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    "友链请求不能为空");
        }
        try {
            return FriendLinkResult.from(repository.insert(
                    NewFriendLink.create(
                            command.name(),
                            command.url(),
                            command.avatarUrl(),
                            command.description(),
                            command.sortOrder(),
                            command.status(),
                            actorId)));
        } catch (IllegalArgumentException exception) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    exception.getMessage());
        }
    }
}
