package com.tyb.myblog.v2.stats.infrastructure.persistence.projection;

import lombok.Getter;
import lombok.Setter;

/** 语言访问 SQL 投影。 */
@Getter
@Setter
public class LanguageTrafficRow {
    private String lang;
    private Long pv;
}
