package com.tyb.myblog.v2.system.domain.siteconfig;

import java.util.Arrays;

/**
 * 站点公开内容支持的语言。
 */
public enum SiteLanguage {
    ZH("zh"),
    JA("ja"),
    EN("en");

    private final String code;

    SiteLanguage(String code) {
        this.code = code;
    }

    /**
     * 获取接口使用的语言代码。
     *
     * @return 小写语言代码
     */
    public String code() {
        return code;
    }

    /**
     * 解析公开接口语言参数。
     *
     * @param value 语言代码
     * @return 对应语言
     */
    public static SiteLanguage parse(String value) {
        if (value == null) {
            throw new IllegalArgumentException("语言不能为空");
        }
        return Arrays.stream(values())
                .filter(language -> language.code.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "语言仅支持 zh、ja、en"));
    }
}
