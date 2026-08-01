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

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(
        webEnvironment =
                SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWebTestClient
class EnrollmentGatewayRoutingSecurityTests {

    private static final String ENROLLMENTS_PATH =
            "/api/v1/enrollments";

    private static final String INTERNAL_ACTIVATE_PATH =
            "/api/v1/enrollments/internal/"
                    + "6000f028-a9ae-4ce1-8c92-cea85744fe58"
                    + "/activate";

    private static final AtomicInteger REQUEST_COUNT =
            new AtomicInteger();

    private static final DisposableServer ENROLLMENT_SERVER =
            startEnrollmentServer();

    private final WebTestClient webTestClient;
    private final SecretKey secretKey;
    private final GatewayJwtProperties jwtProperties;

    @Autowired
    EnrollmentGatewayRoutingSecurityTests(
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
                "ENROLLMENT_SERVICE_URL",
                () -> "http://127.0.0.1:"
                        + ENROLLMENT_SERVER.port()
        );
    }

    @BeforeEach
    void resetRequestCount() {
        REQUEST_COUNT.set(0);
    }

    @AfterAll
    static void stopEnrollmentServer() {
        ENROLLMENT_SERVER.disposeNow();
    }

    @Test
    void rejectsEnrollmentRouteWithoutAccessToken() {
        webTestClient
                .get()
                .uri(ENROLLMENTS_PATH)
                .exchange()
                .expectStatus()
                .isUnauthorized();

        assertThat(REQUEST_COUNT.get()).isZero();
    }

    @Test
    void forwardsEnrollmentRouteWithValidAccessToken() {
        String accessToken = createAccessToken();

        webTestClient
                .get()
                .uri(ENROLLMENTS_PATH + "?page=0&size=20")
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + accessToken
                )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.page")
                .isEqualTo(0)
                .jsonPath("$.content")
                .isArray();

        assertThat(REQUEST_COUNT.get()).isEqualTo(1);
    }

    @Test
    void blocksInternalEnrollmentRoute() {
        String accessToken = createAccessToken();

        webTestClient
                .post()
                .uri(INTERNAL_ACTIVATE_PATH)
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + accessToken
                )
                .exchange()
                .expectStatus()
                .isForbidden();

        assertThat(REQUEST_COUNT.get()).isZero();
    }

    private String createAccessToken() {
        JwtEncoder encoder = NimbusJwtEncoder
                .withSecretKey(secretKey)
                .algorithm(MacAlgorithm.HS256)
                .build();

        Instant issuedAt = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .subject(UUID.randomUUID().toString())
                .audience(
                        List.of(jwtProperties.audience())
                )
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(900))
                .claim(
                        "email",
                        "gateway.student@centerflow.com"
                )
                .claim(
                        "roles",
                        List.of("RECEPTIONIST")
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
    startEnrollmentServer() {
        return HttpServer.create()
                .host("127.0.0.1")
                .port(0)
                .route(routes ->
                        routes
                                .get(
                                        ENROLLMENTS_PATH,
                                        (request, response) -> {
                                            REQUEST_COUNT.incrementAndGet();

                                            String authorization =
                                                    request
                                                            .requestHeaders()
                                                            .get(
                                                                    HttpHeaderNames
                                                                            .AUTHORIZATION
                                                            );

                                            if (authorization == null
                                                    || !authorization
                                                    .startsWith("Bearer ")) {

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
                                                      "content": [],
                                                      "page": 0,
                                                      "size": 20,
                                                      "totalElements": 0,
                                                      "totalPages": 0,
                                                      "first": true,
                                                      "last": true
                                                    }
                                                    """
                                            );
                                        }
                                )
                                .post(
                                        INTERNAL_ACTIVATE_PATH,
                                        (request, response) -> {
                                            REQUEST_COUNT.incrementAndGet();

                                            return jsonResponse(
                                                    response,
                                                    """
                                                    {
                                                      "status": "ACTIVE"
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