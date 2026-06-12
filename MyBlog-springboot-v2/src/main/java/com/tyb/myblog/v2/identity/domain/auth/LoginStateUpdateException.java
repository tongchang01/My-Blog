package com.tyb.myblog.v2.identity.domain.auth;

/**
 * 登录状态没有按预期更新到唯一账号。
 */
public class LoginStateUpdateException extends RuntimeException {

    private LoginStateUpdateException(String message) {
        super(message);
    }

    /**
     * 创建密码失败状态更新异常。
     *
     * @param userId 账号 ID
     * @return 不包含账号凭据的状态更新异常
     */
    public static LoginStateUpdateException passwordFailure(long userId) {
        return new LoginStateUpdateException("登录失败状态更新失败，账号 ID：" + userId);
    }

    /**
     * 创建登录成功状态更新异常。
     *
     * @param userId 账号 ID
     * @return 不包含账号凭据的状态更新异常
     */
    public static LoginStateUpdateException successfulLogin(long userId) {
        return new LoginStateUpdateException("登录成功状态更新失败，账号 ID：" + userId);
    }
}
