package com.tyb.myblog.v2.identity.application.profile;

import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.identity.domain.account.CurrentAccount;
import com.tyb.myblog.v2.identity.domain.account.CurrentAccountRepository;
import com.tyb.myblog.v2.identity.domain.profile.UserProfile;
import com.tyb.myblog.v2.identity.domain.profile.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 查询当前登录账号及其个人资料。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CurrentUserProfileQueryService {

    private final CurrentAccountRepository accountRepository;
    private final UserProfileRepository profileRepository;

    /**
     * 根据认证主体 ID 查询当前用户资料。
     *
     * @param principalId 认证主体中的账号 ID
     * @return 当前账号与个人资料
     */
    public CurrentUserProfileResult query(String principalId) {
        long userId = parsePositiveUserId(principalId);
        Optional<CurrentAccount> account =
                accountRepository.findActiveById(userId);
        Optional<UserProfile> profile =
                profileRepository.findActiveByUserId(userId);

        if (account.isEmpty() || profile.isEmpty()) {
            log.error(
                    "当前用户资料数据不完整，userId={}，accountPresent={}，profilePresent={}",
                    userId,
                    account.isPresent(),
                    profile.isPresent());
            throw new ApiException(ApiErrorCode.INTERNAL_ERROR);
        }

        CurrentAccount currentAccount = account.orElseThrow();
        return new CurrentUserProfileResult(
                currentAccount.id(),
                currentAccount.username(),
                currentAccount.type(),
                UserProfileResult.from(profile.orElseThrow()));
    }

    private long parsePositiveUserId(String principalId) {
        try {
            long userId = Long.parseLong(principalId);
            if (userId <= 0) {
                throw new NumberFormatException();
            }
            return userId;
        } catch (NumberFormatException exception) {
            throw new ApiException(ApiErrorCode.INVALID_TOKEN);
        }
    }
}
