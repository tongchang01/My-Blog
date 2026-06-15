package com.tyb.myblog.v2.content.domain;

/**
 * 支持中文兜底的三语内容名称。
 */
public record ContentName(String zh, String ja, String en) {

    private static final int MAX_LENGTH = 64;

    public static ContentName of(
            String zh,
            String ja,
            String en) {
        return new ContentName(
                required(zh, "中文名称"),
                optional(ja, "日文名称"),
                optional(en, "英文名称"));
    }

    public String localized(ContentLanguage language) {
        if (language == null) {
            throw new IllegalArgumentException("内容语言不能为空");
        }
        return switch (language) {
            case ZH -> zh;
            case JA -> ja == null ? zh : ja;
            case EN -> en == null ? zh : en;
        };
    }

    private static String required(String value, String fieldName) {
        String normalized = optional(value, fieldName);
        if (normalized == null) {
            throw new IllegalArgumentException(
                    fieldName + "不能为空");
        }
        return normalized;
    }

    private static String optional(String value, String fieldName) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    fieldName + "长度不能超过 " + MAX_LENGTH);
        }
        return normalized;
    }
}
