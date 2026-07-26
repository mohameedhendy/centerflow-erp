package com.centerflow.gateway.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtAudienceValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(
        GatewayJwtProperties.class
)
public class GatewayJwtConfiguration {

    private static final String HMAC_SHA_256 =
            "HmacSHA256";

    @Bean
    SecretKey gatewayJwtSecretKey(
            GatewayJwtProperties properties
    ) {
        return new SecretKeySpec(
                properties.secret()
                        .getBytes(StandardCharsets.UTF_8),
                HMAC_SHA_256
        );
    }

    @Bean
    ReactiveJwtDecoder gatewayJwtDecoder(
            SecretKey secretKey,
            GatewayJwtProperties properties
    ) {
        NimbusReactiveJwtDecoder decoder =
                NimbusReactiveJwtDecoder
                        .withSecretKey(secretKey)
                        .macAlgorithm(MacAlgorithm.HS256)
                        .build();

        OAuth2TokenValidator<Jwt> defaultValidators =
                JwtValidators.createDefaultWithIssuer(
                        properties.issuer()
                );

        OAuth2TokenValidator<Jwt> audienceValidator =
                new JwtAudienceValidator(
                        properties.audience()
                );

        OAuth2TokenValidator<Jwt> validator =
                new DelegatingOAuth2TokenValidator<>(
                        defaultValidators,
                        audienceValidator
                );

        decoder.setJwtValidator(validator);

        return decoder;
    }
}