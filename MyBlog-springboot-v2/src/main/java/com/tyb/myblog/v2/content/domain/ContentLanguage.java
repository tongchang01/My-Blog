package com.tyb.myblog.v2.content.domain;

import java.util.Arrays;

/**
 * 公开内容支持的语言。
 */
public enum ContentLanguage {
    ZH("zh"),
    JA("ja"),
    EN("en");

    private final String code;

    ContentLanguage(String code) {
        this.code = code;
    }

    public static ContentLanguage parse(String value) {
        return Arrays.stream(values())
                .filter(language -> language.code.equals(value))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("不支持的内容语言"));
    }
}
