package com.centerflow.identity.security.jwt;

import com.centerflow.identity.security.config.JwtProperties;
import com.centerflow.identity.security.user.IdentityUserPrincipal;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class JwtAccessTokenService {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;
    private final Clock clock;

    public JwtAccessTokenService(
            JwtEncoder jwtEncoder,
            JwtProperties properties,
            Clock clock
    ) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
        this.clock = clock;
    }

    public AccessTokenResult issueToken(
            IdentityUserPrincipal principal
    ) {
        Instant issuedAt = Instant.now(clock);
        Instant expiresAt = issuedAt.plus(
                properties.accessTokenTtl()
        );

        List<String> roleNames = principal
                .getRoles()
                .stream()
                .map(Enum::name)
                .toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(principal.getUserId().toString())
                .audience(
                        List.of(properties.audience())
                )
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim(
                        "jti",
                        UUID.randomUUID().toString()
                )
                .claim(
                        "email",
                        principal.getUsername()
                )
                .claim(
                        "roles",
                        roleNames
                )
                .build();

        JwsHeader header = JwsHeader
                .with(MacAlgorithm.HS256)
                .type("JWT")
                .build();

        String tokenValue = jwtEncoder
                .encode(
                        JwtEncoderParameters.from(
                                header,
                                claims
                        )
                )
                .getTokenValue();

        return new AccessTokenResult(
                tokenValue,
                issuedAt,
                expiresAt
        );
    }
}