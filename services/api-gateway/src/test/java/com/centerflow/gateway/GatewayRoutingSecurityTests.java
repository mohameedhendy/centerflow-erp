package com.centerflow.gateway;

import com.centerflow.gateway.security.GatewayJwtProperties;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerResponse;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ActiveProfiles("test")
@SpringBootTest(
        webEnvironment =
                SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWebTestClient
class GatewayRoutingSecurityTests {

    private static final String LOGIN_PATH =
            "/api/v1/auth/login";

    private static final String CURRENT_USER_PATH =
            "/api/v1/auth/me";

    private static final DisposableServer
            IDENTITY_SERVER = startIdentityServer();

    private final WebTestClient webTestClient;
    private final SecretKey secretKey;
    private final GatewayJwtProperties jwtProperties;

    @Autowired
    GatewayRoutingSecurityTests(
            WebTestClient webTestClient,
            SecretKey secretKey,
            GatewayJwtProperties jwtProperties
    ) {
        this.webTestClient = webTestClient;
        this.secretKey = secretKey;
        this.jwtProperties = jwtProperties;
    }

    @DynamicPropertySource
    static void gatewayProperties(
            DynamicPropertyRegistry registry
    ) {
        registry.add(
                "IDENTITY_SERVICE_URL",
                () -> "http://127.0.0.1:"
                        + IDENTITY_SERVER.port()
        );
    }

    @AfterAll
    static void stopIdentityServer() {
        IDENTITY_SERVER.disposeNow();
    }

    @Test
    void forwardsPublicLoginWithoutAccessToken() {
        webTestClient
                .post()
                .uri(LOGIN_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(
                        """
                        {
                          "email": "gateway.student@centerflow.com",
                          "password": "Student123"
                        }
                        """
                )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.forwarded")
                .isEqualTo(true);
    }

    @Test
    void rejectsProtectedRouteWithoutAccessToken() {
        webTestClient
                .get()
                .uri(CURRENT_USER_PATH)
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }

    @Test
    void forwardsProtectedRouteWithValidAccessToken() {
        String accessToken = createAccessToken(
                jwtProperties.audience()
        );

        webTestClient
                .get()
                .uri(CURRENT_USER_PATH)
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + accessToken
                )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.email")
                .isEqualTo(
                        "gateway.student@centerflow.com"
                )
                .jsonPath("$.roles[0]")
                .isEqualTo("STUDENT");
    }

    @Test
    void rejectsAccessTokenWithWrongAudience() {
        String accessToken = createAccessToken(
                "another-api"
        );

        webTestClient
                .get()
                .uri(CURRENT_USER_PATH)
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + accessToken
                )
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }

    private String createAccessToken(
            String audience
    ) {
        JwtEncoder encoder = NimbusJwtEncoder
                .withSecretKey(secretKey)
                .algorithm(MacAlgorithm.HS256)
                .build();

        Instant issuedAt = Instant.now();
        Instant expiresAt =
                issuedAt.plusSeconds(900);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .subject(
                        UUID.randomUUID().toString()
                )
                .audience(List.of(audience))
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim(
                        "email",
                        "gateway.student@centerflow.com"
                )
                .claim(
                        "roles",
                        List.of("STUDENT")
                )
                .build();

        JwsHeader header = JwsHeader
                .with(MacAlgorithm.HS256)
                .type("JWT")
                .build();

        return encoder
                .encode(
                        JwtEncoderParameters.from(
                                header,
                                claims
                        )
                )
                .getTokenValue();
    }

    private static DisposableServer
    startIdentityServer() {
        return HttpServer.create()
                .host("127.0.0.1")
                .port(0)
                .route(routes ->
                        routes
                                .post(
                                        LOGIN_PATH,
                                        (request, response) ->
                                                request.receive()
                                                        .then(
                                                                jsonResponse(
                                                                        response,
                                                                        """
                                                                        {
                                                                          "forwarded": true
                                                                        }
                                                                        """
                                                                )
                                                        )
                                )

                                .get(
                                        CURRENT_USER_PATH,
                                        (request, response) -> {
                                            String authorization =
                                                    request
                                                            .requestHeaders()
                                                            .get(
                                                                    HttpHeaderNames.AUTHORIZATION
                                                            );

                                            if (authorization == null
                                                    || !authorization
                                                    .startsWith(
                                                            "Bearer "
                                                    )) {

                                                return response
                                                        .status(
                                                                HttpResponseStatus
                                                                        .INTERNAL_SERVER_ERROR
                                                        )
                                                        .send()
                                                        .then();
                                            }

                                            return jsonResponse(
                                                    response,
                                                    """
                                                    {
                                                      "id": "6000f028-a9ae-4ce1-8c92-cea85744fe58",
                                                      "email": "gateway.student@centerflow.com",
                                                      "roles": ["STUDENT"]
                                                    }
                                                    """
                                            );
                                        }
                                )
                )
                .bindNow();
    }

    private static Mono<Void> jsonResponse(
            HttpServerResponse response,
            String body
    ) {
        return response
                .status(HttpResponseStatus.OK)
                .header(
                        HttpHeaderNames.CONTENT_TYPE,
                        MediaType.APPLICATION_JSON_VALUE
                )
                .sendString(Mono.just(body))
                .then();
    }
}