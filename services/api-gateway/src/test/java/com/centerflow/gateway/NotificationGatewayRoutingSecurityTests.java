package com.centerflow.gateway;

import com.centerflow.gateway.security.GatewayJwtProperties;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(
        webEnvironment =
                SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWebTestClient
class NotificationGatewayRoutingSecurityTests {

    private static final String NOTIFICATIONS_PATH =
            "/api/v1/notifications";

    private static final String INTERNAL_PREFIX =
            "/api/v1/notifications/internal/";

    private static final AtomicReference<String>
            FORWARDED_USER_ID =
            new AtomicReference<>();

    private static final AtomicInteger
            INTERNAL_REQUEST_COUNT =
            new AtomicInteger();

    private static final DisposableServer
            NOTIFICATION_SERVER =
            startNotificationServer();

    private final WebTestClient webTestClient;
    private final SecretKey secretKey;
    private final GatewayJwtProperties jwtProperties;

    @Autowired
    NotificationGatewayRoutingSecurityTests(
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
                "NOTIFICATION_SERVICE_URL",
                () -> "http://127.0.0.1:"
                        + NOTIFICATION_SERVER.port()
        );
    }

    @BeforeEach
    void resetRequestState() {
        FORWARDED_USER_ID.set(null);
        INTERNAL_REQUEST_COUNT.set(0);
    }

    @AfterAll
    static void stopNotificationServer() {
        NOTIFICATION_SERVER.disposeNow();
    }

    @Test
    void rejectsNotificationRouteWithoutToken() {
        webTestClient
                .get()
                .uri(NOTIFICATIONS_PATH)
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }

    @Test
    void injectsJwtSubjectAndOverwritesSpoofedHeader() {
        UUID authenticatedUserId =
                UUID.randomUUID();

        UUID spoofedUserId =
                UUID.randomUUID();

        String token = createAccessToken(
                authenticatedUserId
        );

        webTestClient
                .get()
                .uri(NOTIFICATIONS_PATH)
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + token
                )
                .header(
                        "X-User-Id",
                        spoofedUserId.toString()
                )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.forwardedUserId")
                .isEqualTo(
                        authenticatedUserId.toString()
                );

        assertThat(FORWARDED_USER_ID.get())
                .isEqualTo(
                        authenticatedUserId.toString()
                );
    }

    @Test
    void blocksInternalNotificationRoute() {
        String token = createAccessToken(
                UUID.randomUUID()
        );

        webTestClient
                .post()
                .uri(
                        INTERNAL_PREFIX
                                + "notifications"
                )
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + token
                )
                .exchange()
                .expectStatus()
                .isForbidden();

        assertThat(INTERNAL_REQUEST_COUNT.get())
                .isZero();
    }

    private String createAccessToken(
            UUID userId
    ) {
        JwtEncoder encoder = NimbusJwtEncoder
                .withSecretKey(secretKey)
                .algorithm(MacAlgorithm.HS256)
                .build();

        Instant issuedAt = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .subject(userId.toString())
                .audience(
                        List.of(
                                jwtProperties.audience()
                        )
                )
                .issuedAt(issuedAt)
                .expiresAt(
                        issuedAt.plusSeconds(900)
                )
                .claim(
                        "email",
                        "notification.gateway@centerflow.com"
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
    startNotificationServer() {
        return HttpServer.create()
                .host("127.0.0.1")
                .port(0)
                .route(routes ->
                        routes
                                .get(
                                        NOTIFICATIONS_PATH,
                                        (request, response) -> {
                                            String userId =
                                                    request
                                                            .requestHeaders()
                                                            .get(
                                                                    "X-User-Id"
                                                            );

                                            FORWARDED_USER_ID.set(
                                                    userId
                                            );

                                            return jsonResponse(
                                                    response,
                                                    """
                                                    {
                                                      "forwardedUserId": "%s"
                                                    }
                                                    """.formatted(
                                                            userId
                                                    )
                                            );
                                        }
                                )
                                .route(
                                        request ->
                                                request.uri()
                                                        .startsWith(
                                                                INTERNAL_PREFIX
                                                        ),
                                        (request, response) -> {
                                            INTERNAL_REQUEST_COUNT
                                                    .incrementAndGet();

                                            return jsonResponse(
                                                    response,
                                                    """
                                                    {
                                                      "internalForwarded": true
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