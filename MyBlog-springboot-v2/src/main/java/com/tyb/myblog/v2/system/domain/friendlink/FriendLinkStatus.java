package com.tyb.myblog.v2.system.domain.friendlink;

/**
 * 友链展示状态。
 */
public enum FriendLinkStatus {
    VISIBLE(1),
    HIDDEN(2);

    private final int databaseValue;

    FriendLinkStatus(int databaseValue) {
        this.databaseValue = databaseValue;
    }

    public int databaseValue() {
        return databaseValue;
    }

    public static FriendLinkStatus fromDatabaseValue(Integer value) {
        if (value == null) {
            throw new IllegalArgumentException("友链状态不能为空");
        }
        return switch (value) {
            case 1 -> VISIBLE;
            case 2 -> HIDDEN;
            default -> throw new IllegalArgumentException(
                    "不支持的友链状态值");
        };
    }
}
