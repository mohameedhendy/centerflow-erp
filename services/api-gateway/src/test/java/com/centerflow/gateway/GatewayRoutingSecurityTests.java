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
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

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

    private static final String ACADEMIC_BRANCHES_PATH =
            "/api/v1/academic/branches";

    private static final String INTERNAL_ACADEMIC_PATH =
            "/api/v1/academic/internal/batches/"
                    + "00000000-0000-0000-0000-000000000001"
                    + "/seat-availability";

    private static final AtomicInteger
            INTERNAL_ACADEMIC_REQUESTS =
            new AtomicInteger();

    private static final DisposableServer
            IDENTITY_SERVER = startIdentityServer();

    private static final DisposableServer
            ACADEMIC_SERVER = startAcademicServer();

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

        registry.add(
                "ACADEMIC_SERVICE_URL",
                () -> "http://127.0.0.1:"
                        + ACADEMIC_SERVER.port()
        );
    }

    @AfterAll
    static void stopMockServices() {
        IDENTITY_SERVER.disposeNow();
        ACADEMIC_SERVER.disposeNow();
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
    void rejectsProtectedIdentityRouteWithoutAccessToken() {
        webTestClient
                .get()
                .uri(CURRENT_USER_PATH)
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }

    @Test
    void forwardsProtectedIdentityRouteWithValidAccessToken() {
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

    @Test
    void rejectsAcademicRouteWithoutAccessToken() {
        webTestClient
                .get()
                .uri(ACADEMIC_BRANCHES_PATH)
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }

    @Test
    void rejectsAcademicRouteWithMalformedAccessToken() {
        webTestClient
                .get()
                .uri(ACADEMIC_BRANCHES_PATH)
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer invalid-token"
                )
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }

    @Test
    void forwardsAcademicReadRouteWithValidAccessToken() {
        String accessToken = createAccessToken(
                jwtProperties.audience()
        );

        webTestClient
                .get()
                .uri(ACADEMIC_BRANCHES_PATH)
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + accessToken
                )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.service")
                .isEqualTo("academic")
                .jsonPath("$.operation")
                .isEqualTo("read")
                .jsonPath("$.authorizationForwarded")
                .isEqualTo(true);
    }

    @Test
    void forwardsAcademicWriteRouteWithValidAccessToken() {
        String accessToken = createAccessToken(
                jwtProperties.audience()
        );

        webTestClient
                .post()
                .uri(ACADEMIC_BRANCHES_PATH)
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + accessToken
                )
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(
                        """
                        {
                          "code": "CAIRO",
                          "name": "Cairo Branch",
                          "city": "Cairo"
                        }
                        """
                )
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody()
                .jsonPath("$.service")
                .isEqualTo("academic")
                .jsonPath("$.operation")
                .isEqualTo("write")
                .jsonPath("$.authorizationForwarded")
                .isEqualTo(true);
    }

    @Test
    void blocksInternalAcademicRouteWithValidAccessToken() {
        String accessToken = createAccessToken(
                jwtProperties.audience()
        );

        int requestsBefore =
                INTERNAL_ACADEMIC_REQUESTS.get();

        webTestClient
                .get()
                .uri(INTERNAL_ACADEMIC_PATH)
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + accessToken
                )
                .exchange()
                .expectStatus()
                .isForbidden();

        assertThat(
                INTERNAL_ACADEMIC_REQUESTS.get()
        ).isEqualTo(requestsBefore);
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
                        List.of("ADMIN")
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
                                                                        HttpResponseStatus.OK,
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

                                            if (!hasBearerToken(
                                                    authorization
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
                                                    HttpResponseStatus.OK,
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

    private static DisposableServer
    startAcademicServer() {
        return HttpServer.create()
                .host("127.0.0.1")
                .port(0)
                .route(routes ->
                        routes
                                .get(
                                        ACADEMIC_BRANCHES_PATH,
                                        (request, response) -> {
                                            String authorization =
                                                    request
                                                            .requestHeaders()
                                                            .get(
                                                                    HttpHeaderNames.AUTHORIZATION
                                                            );

                                            if (!hasBearerToken(
                                                    authorization
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
                                                    HttpResponseStatus.OK,
                                                    """
                                                    {
                                                      "service": "academic",
                                                      "operation": "read",
                                                      "authorizationForwarded": true
                                                    }
                                                    """
                                            );
                                        }
                                )

                                .post(
                                        ACADEMIC_BRANCHES_PATH,
                                        (request, response) -> {
                                            String authorization =
                                                    request
                                                            .requestHeaders()
                                                            .get(
                                                                    HttpHeaderNames.AUTHORIZATION
                                                            );

                                            if (!hasBearerToken(
                                                    authorization
                                            )) {
                                                return response
                                                        .status(
                                                                HttpResponseStatus
                                                                        .INTERNAL_SERVER_ERROR
                                                        )
                                                        .send()
                                                        .then();
                                            }

                                            return request.receive()
                                                    .then(
                                                            jsonResponse(
                                                                    response,
                                                                    HttpResponseStatus.CREATED,
                                                                    """
                                                                    {
                                                                      "service": "academic",
                                                                      "operation": "write",
                                                                      "authorizationForwarded": true
                                                                    }
                                                                    """
                                                            )
                                                    );
                                        }
                                )

                                .get(
                                        INTERNAL_ACADEMIC_PATH,
                                        (request, response) -> {
                                            INTERNAL_ACADEMIC_REQUESTS
                                                    .incrementAndGet();

                                            return jsonResponse(
                                                    response,
                                                    HttpResponseStatus.OK,
                                                    """
                                                    {
                                                      "internalReached": true
                                                    }
                                                    """
                                            );
                                        }
                                )
                )
                .bindNow();
    }

    private static boolean hasBearerToken(
            String authorization
    ) {
        return authorization != null
                && authorization.startsWith(
                "Bearer "
        );
    }

    private static Mono<Void> jsonResponse(
            HttpServerResponse response,
            HttpResponseStatus status,
            String body
    ) {
        return response
                .status(status)
                .header(
                        HttpHeaderNames.CONTENT_TYPE,
                        MediaType.APPLICATION_JSON_VALUE
                )
                .sendString(Mono.just(body))
                .then();
    }
}