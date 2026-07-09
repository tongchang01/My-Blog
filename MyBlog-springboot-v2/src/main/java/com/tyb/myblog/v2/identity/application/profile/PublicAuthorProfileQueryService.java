package com.tyb.myblog.v2.identity.application.profile;

import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.identity.domain.profile.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * 查询前台公开作者资料。
 */
@Service
@RequiredArgsConstructor
public class PublicAuthorProfileQueryService {

    private final UserProfileRepository repository;
    private final Clock clock;

    /**
     * 返回当前公开文章的主作者资料。
     */
    public UserProfileResult query() {
        return repository.findPrimaryPublicAuthor(LocalDateTime.now(clock))
                .map(UserProfileResult::from)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
    }
}
