package com.tyb.myblog.v2.identity.domain.account;

import java.util.Arrays;

/**
 * 登录账号类型。
 *
 * @param databaseValue 数据库存储值
 * @param canLoginToAdmin 是否允许登录后台
 */
public enum AccountType {

    /**
     * 管理员账号。
     */
    ADMIN(1, true),

    /**
     * 演示账号。
     */
    DEMO(2, true),

    /**
     * 访客账号。
     */
    GUEST(3, false);

    private final int databaseValue;
    private final boolean canLoginToAdmin;

    AccountType(int databaseValue, boolean canLoginToAdmin) {
        this.databaseValue = databaseValue;
        this.canLoginToAdmin = canLoginToAdmin;
    }

    /**
     * 获取数据库存储值。
     *
     * @return 数据库存储值
     */
    public int databaseValue() {
        return databaseValue;
    }

    /**
     * 判断账号类型是否允许登录后台。
     *
     * @return 允许登录时返回 {@code true}
     */
    public boolean canLoginToAdmin() {
        return canLoginToAdmin;
    }

    /**
     * 根据数据库存储值还原账号类型。
     *
     * @param databaseValue 数据库存储值
     * @return 对应的账号类型
     * @throws IllegalArgumentException 数据库存储值未知时抛出
     */
    public static AccountType fromDatabaseValue(int databaseValue) {
        return Arrays.stream(values())
                .filter(type -> type.databaseValue == databaseValue)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知账号类型：" + databaseValue));
    }
}
