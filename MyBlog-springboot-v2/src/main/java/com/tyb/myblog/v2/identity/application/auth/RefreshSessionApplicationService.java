package com.tyb.myblog.v2.identity.application.auth;

import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 处理 refresh 请求参数并统一对外失败语义。
 */
@Service
@RequiredArgsConstructor
public class RefreshSessionApplicationService {

    private final RefreshSessionTransactionService transactionService;

    /**
     * 使用一枚 refresh token 轮换出新的认证会话。
     */
    public LoginTokenResult refresh(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new ApiException(ApiErrorCode.INVALID_TOKEN);
        }
        return transactionService.refresh(refreshToken)
                .orElseThrow(() ->
                        new ApiException(ApiErrorCode.INVALID_TOKEN));
    }
}
