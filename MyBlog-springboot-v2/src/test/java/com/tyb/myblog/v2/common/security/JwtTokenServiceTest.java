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
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class JwtTokenServiceTest {

    private static final String ISSUER = "myblog-v2-test";
    private static final String SECRET = "test-secret-test-secret-test-secret-123456";

    private final JwtTokenService tokenService = new JwtTokenService(
            new SecurityJwtProperties(ISSUER, SECRET, Duration.ofMinutes(15)));

    @Test
    void issuesAndParsesAccessToken() {
        var token = tokenService.issueAccessToken("user-1", "admin@example.com", List.of("ADMIN"), 3);
        var claims = tokenService.verify(token.accessToken()).orElseThrow();

        assertThat(claims.userId()).isEqualTo("user-1");
        assertThat(claims.username()).isEqualTo("admin@example.com");
        assertThat(claims.roles()).containsExactly("ADMIN");
        assertThat(claims.tokenVersion()).isEqualTo(3);
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

        assertThat(tokenService.verify(token)).isEmpty();
    }

    @Test
    void rejectsArticleAccessTokenAsLoginAccessToken() {
        String token = signedToken(claims -> claims.claim("typ", "article_access"));

        assertThat(tokenService.verify(token)).isEmpty();
    }

    @Test
    void rejectsTokenWithoutIntegerVersion() {
        String token = signedToken(claims -> claims.claim("ver", "0"));

        assertThat(tokenService.verify(token)).isEmpty();
    }

    @Test
    void rejectsExpiredToken() {
        Instant now = Instant.now();
        String token = signedToken(claims -> claims
                .issuedAt(now.minusSeconds(120))
                .expiresAt(now.minusSeconds(60)));

        assertThat(tokenService.verify(token)).isEmpty();
    }

    private String signedToken(Consumer<JwtClaimsSet.Builder> customization) {
        Instant now = Instant.now();
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
