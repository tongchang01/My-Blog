package com.tyb.myblog.v2.content.domain.article;

/**
 * 首页展示槽位。
 */
public enum HomepageSlot {
    NONE(0),
    PINNED(1),
    FEATURED(2);

    private final int limit;

    HomepageSlot(int limit) {
        this.limit = limit;
    }

    public int limit() {
        return limit;
    }

    public static HomepageSlot normalize(HomepageSlot slot) {
        return slot == null ? NONE : slot;
    }
}
