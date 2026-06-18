package com.tyb.myblog.v2.stats.domain;

import java.time.LocalDate;

/**
 * 不可逆访客标识生成端口。
 */
public interface VisitorHashGenerator {

    String hash(LocalDate date, String clientIp, String userAgent);
}
