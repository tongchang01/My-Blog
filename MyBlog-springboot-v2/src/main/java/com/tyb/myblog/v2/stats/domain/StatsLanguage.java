package com.tyb.myblog.v2.stats.domain;

import java.util.Arrays;

/**
 * 访问统计支持的站点语言。
 */
public enum StatsLanguage {

    ZH("zh"),
    JA("ja"),
    EN("en");

    private final String code;

    StatsLanguage(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static StatsLanguage fromCode(String value) {
        return Arrays.stream(values())
                .filter(language -> language.code.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "统计语言只允许 zh、ja 或 en"));
    }
}
