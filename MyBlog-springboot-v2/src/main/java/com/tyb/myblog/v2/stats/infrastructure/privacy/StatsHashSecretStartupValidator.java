package com.tyb.myblog.v2.stats.infrastructure.privacy;

import com.tyb.myblog.v2.stats.infrastructure.config.StatsProperties;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

/**
 * local/prod 运行环境的访问统计密钥启动校验。
 */
@Component
@Profile({"local", "prod"})
public class StatsHashSecretStartupValidator
        implements InitializingBean {

    private final StatsProperties properties;

    public StatsHashSecretStartupValidator(
            StatsProperties properties) {
        this.properties = properties;
    }

    @Override
    public void afterPropertiesSet() {
        String secret = properties.hashSecret();
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException(
                    "统计访客标识密钥不能为空，请配置 MYBLOG_STATS_HASH_SECRET");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "统计访客标识密钥按 UTF-8 编码后不能少于 32 字节");
        }
    }
}
