package com.tyb.myblog.v2.stats.infrastructure.privacy;

import com.tyb.myblog.v2.stats.domain.VisitorHashGenerator;
import com.tyb.myblog.v2.stats.infrastructure.config.StatsProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.util.HexFormat;

/**
 * 使用每日轮换输入的 HMAC-SHA256 生成访客标识。
 */
@Component
public class HmacVisitorHashGenerator implements VisitorHashGenerator {

    private static final String ALGORITHM = "HmacSHA256";

    private final byte[] secret;

    public HmacVisitorHashGenerator(StatsProperties properties) {
        this.secret = normalize(properties.hashSecret())
                .getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String hash(
            LocalDate date,
            String clientIp,
            String userAgent) {
        String payload = date + "\n"
                + normalize(clientIp) + "\n"
                + normalize(userAgent);
        try {
            // Mac 不是线程安全对象，每次调用独立创建，避免并发请求互相污染状态。
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret, ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(
                    payload.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(
                    "无法生成访问统计访客标识",
                    exception);
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
