package com.tyb.myblog.v2.common.security.auth;

import com.tyb.myblog.v2.common.auth.token.AccessTokenIssuer;
import com.tyb.myblog.v2.common.auth.token.AccessTokenVerifier;
import com.tyb.myblog.v2.common.auth.token.TokenClaims;
import com.tyb.myblog.v2.common.auth.token.TokenPair;
import com.tyb.myblog.v2.common.config.SecurityJwtProperties;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JWT 访问令牌服务。
 *
 * <p>负责签发、解析和撤销访问令牌。当前实现使用 HS256 对称签名，
 * 因此生产环境必须保护好 {@link SecurityJwtProperties#secret()}，不能提交到 Git。</p>
 */
@Service
public class JwtTokenService implements AccessTokenIssuer, AccessTokenVerifier {

    private static final String ACCESS_TOKEN_TYPE = "access";

    /**
     * JWT HMAC 签名算法。
     */
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    /**
     * JWT 配置项。
     */
    private final SecurityJwtProperties properties;
    /**
     * token 撤销存储。
     */
    private final TokenRevocationStore revocationStore;
    /**
     * JWT 编码器。
     */
    private final JwtEncoder jwtEncoder;
    /**
     * JWT 解码器。
     */
    private final JwtDecoder jwtDecoder;

    /**
     * 创建 JWT 服务。
     *
     * @param properties      JWT 配置项
     * @param revocationStore token 撤销存储
     */
    public JwtTokenService(SecurityJwtProperties properties, TokenRevocationStore revocationStore) {
        this.properties = properties;
        this.revocationStore = revocationStore;
        SecretKey secretKey = new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
        this.jwtEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.issuer()));
        this.jwtDecoder = decoder;
    }

    /**
     * 签发访问令牌。
     *
     * @param userId   用户 ID
     * @param username 登录用户名
     * @param roles    用户角色名称列表
     * @param tokenVersion 用户当前 token 版本
     * @return 访问令牌和过期时间
     */
    @Override
    public TokenPair issueAccessToken(String userId, String username, List<String> roles, int tokenVersion) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(properties.accessTokenTtl());
        String tokenId = UUID.randomUUID().toString();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .id(tokenId)
                .subject(userId)
                .claim("typ", ACCESS_TOKEN_TYPE)
                .claim("ver", tokenVersion)
                .claim("username", username)
                .claim("roles", roles)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenPair(accessToken, expiresAt);
    }

    /**
     * 解析访问令牌。
     *
     * <p>签名无效、令牌过期、格式错误或已撤销时统一返回空，避免认证过滤器泄露具体失败原因。</p>
     *
     * @param token 原始访问令牌
     * @return 解析后的声明
     */
    @Override
    public Optional<TokenClaims> verify(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            Integer tokenVersion = readTokenVersion(jwt);
            if (!hasRequiredAccessClaims(jwt, tokenVersion) || revocationStore.isRevoked(jwt.getId())) {
                return Optional.empty();
            }
            return Optional.of(new TokenClaims(
                    jwt.getId(),
                    jwt.getSubject(),
                    jwt.getClaimAsString("username"),
                    readRoles(jwt),
                    tokenVersion,
                    jwt.getExpiresAt()
            ));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    /**
     * 撤销访问令牌。
     *
     * <p>当前用于登出场景。撤销记录只需要保存到 token 原始过期时间，过期后可清理。</p>
     *
     * @param token 原始访问令牌
     */
    public void revoke(String token) {
        verify(token).ifPresent(claims -> revocationStore.revoke(claims.tokenId(), claims.expiresAt()));
    }

    /**
     * 读取 JWT 中的业务角色列表。
     */
    private List<String> readRoles(Jwt jwt) {
        List<String> roleNames = jwt.getClaimAsStringList("roles");
        if (roleNames == null) {
            return List.of();
        }
        return List.copyOf(roleNames);
    }

    private boolean hasRequiredAccessClaims(Jwt jwt, Integer tokenVersion) {
        return ACCESS_TOKEN_TYPE.equals(jwt.getClaimAsString("typ"))
                && jwt.getId() != null
                && jwt.getSubject() != null
                && jwt.getIssuedAt() != null
                && jwt.getExpiresAt() != null
                && tokenVersion != null;
    }

    private Integer readTokenVersion(Jwt jwt) {
        Object claim = jwt.getClaim("ver");
        if (!(claim instanceof Number number)) {
            return null;
        }
        double value = number.doubleValue();
        int integerValue = number.intValue();
        if (integerValue < 0 || value != integerValue) {
            return null;
        }
        return integerValue;
    }
}
