package com.aurora.myblog.v2.common.security.auth;

import com.aurora.myblog.v2.common.config.SecurityJwtProperties;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
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

@Service
public class JwtTokenService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final SecurityJwtProperties properties;
    private final TokenRevocationStore revocationStore;
    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;

    public JwtTokenService(SecurityJwtProperties properties, TokenRevocationStore revocationStore) {
        this.properties = properties;
        this.revocationStore = revocationStore;
        SecretKey secretKey = new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
        this.jwtEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
        this.jwtDecoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    public TokenPair issueAccessToken(String userId, String username, List<String> roles) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(properties.accessTokenTtl());
        String tokenId = UUID.randomUUID().toString();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .id(tokenId)
                .subject(userId)
                .claim("username", username)
                .claim("roles", roles)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenPair(accessToken, expiresAt);
    }

    public Optional<TokenClaims> parse(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            if (revocationStore.isRevoked(jwt.getId())) {
                return Optional.empty();
            }
            return Optional.of(new TokenClaims(
                    jwt.getId(),
                    jwt.getSubject(),
                    jwt.getClaimAsString("username"),
                    readRoles(jwt),
                    jwt.getExpiresAt()
            ));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    public void revoke(String token) {
        parse(token).ifPresent(claims -> revocationStore.revoke(claims.tokenId(), claims.expiresAt()));
    }

    private List<String> readRoles(Jwt jwt) {
        List<String> roleNames = jwt.getClaimAsStringList("roles");
        if (roleNames == null) {
            return List.of();
        }
        return List.copyOf(roleNames);
    }
}
