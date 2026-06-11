package com.tyb.myblog.v2.common.security;

import com.tyb.myblog.v2.common.config.SecurityJwtProperties;
import com.tyb.myblog.v2.common.security.auth.JwtTokenService;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class JwtTokenServiceTest {

    private static final String ISSUER = "myblog-v2-test";
    private static final String SECRET = "test-secret-test-secret-test-secret-123456";
    private static final Instant FIXED_INSTANT = Instant.parse("2026-06-11T12:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, java.time.ZoneOffset.UTC);
    private static final SecurityJwtProperties PROPERTIES =
            new SecurityJwtProperties(ISSUER, SECRET, Duration.ofMinutes(15), Duration.ofDays(7));

    private final JwtTokenService tokenService = new JwtTokenService(PROPERTIES, FIXED_CLOCK);

    /**
     * 验证 access token 的签发与过期时间完全由应用统一 Clock 决定。
     */
    @Test
    void issuesAndParsesAccessToken() {
        var token = tokenService.issueAccessToken("user-1", "admin@example.com", List.of("ADMIN"), 3);
        var claims = tokenService.decode(token.accessToken()).orElseThrow();

        assertThat(token.expiresAt()).isEqualTo(FIXED_INSTANT.plus(Duration.ofMinutes(15)));
        assertThat(claims.userId()).isEqualTo("user-1");
        assertThat(claims.username()).isEqualTo("admin@example.com");
        assertThat(claims.roles()).containsExactly("ADMIN");
        assertThat(claims.tokenVersion()).isEqualTo(3);
    }

    /**
     * 验证解码时同样使用应用统一 Clock，超过默认时钟偏差容限后拒绝旧 token。
     */
    @Test
    void rejectsTokenAfterInjectedClockReachesExpiration() {
        var token = tokenService.issueAccessToken("user-1", "admin@example.com", List.of("ADMIN"), 3);
        Clock expiredClock = Clock.fixed(
                FIXED_INSTANT.plus(Duration.ofMinutes(17)),
                java.time.ZoneOffset.UTC);
        JwtTokenService expiredTokenService = new JwtTokenService(PROPERTIES, expiredClock);

        assertThat(expiredTokenService.decode(token.accessToken())).isEmpty();
    }

    @Test
    void doesNotExposeLegacyInMemoryRevocationTypes() {
        assertThatCode(() -> Class.forName(
                "com.tyb.myblog.v2.common.security.auth.TokenRevocationStore"))
                .isInstanceOf(ClassNotFoundException.class);
        assertThatCode(() -> Class.forName(
                "com.tyb.myblog.v2.common.security.support.InMemoryTokenRevocationStore"))
                .isInstanceOf(ClassNotFoundException.class);
    }

    @Test
    void rejectsTokenFromWrongIssuer() {
        String token = signedToken(claims -> claims.issuer("another-service"));

        assertThat(tokenService.decode(token)).isEmpty();
    }

    @Test
    void rejectsArticleAccessTokenAsLoginAccessToken() {
        String token = signedToken(claims -> claims.claim("typ", "article_access"));

        assertThat(tokenService.decode(token)).isEmpty();
    }

    @Test
    void rejectsTokenWithoutIntegerVersion() {
        String token = signedToken(claims -> claims.claim("ver", "0"));

        assertThat(tokenService.decode(token)).isEmpty();
    }

    @Test
    void rejectsExpiredToken() {
        Instant now = FIXED_INSTANT;
        String token = signedToken(claims -> claims
                .issuedAt(now.minusSeconds(120))
                .expiresAt(now.minusSeconds(61)));

        assertThat(tokenService.decode(token)).isEmpty();
    }

    private String signedToken(Consumer<JwtClaimsSet.Builder> customization) {
        Instant now = FIXED_INSTANT;
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(900))
                .id("test-token-id")
                .subject("user-1")
                .claim("typ", "access")
                .claim("ver", 0)
                .claim("username", "admin@example.com")
                .claim("roles", List.of("ADMIN"));
        customization.accept(claims);

        var key = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        var encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        return encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),
                claims.build())).getTokenValue();
    }
}
