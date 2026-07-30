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
class FinanceGatewayRoutingSecurityTests {

    private static final String PRICING_PLANS_PATH =
            "/api/v1/finance/pricing-plans";

    private static final String INTERNAL_PREFIX =
            "/api/v1/finance/internal/";

    private static final AtomicInteger
            INTERNAL_REQUEST_COUNT =
            new AtomicInteger();

    private static final DisposableServer
            FINANCE_SERVER = startFinanceServer();

    private final WebTestClient webTestClient;
    private final SecretKey secretKey;
    private final GatewayJwtProperties jwtProperties;

    @Autowired
    FinanceGatewayRoutingSecurityTests(
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
                "FINANCE_SERVICE_URL",
                () -> "http://127.0.0.1:"
                        + FINANCE_SERVER.port()
        );
    }

    @BeforeEach
    void resetInternalRequestCount() {
        INTERNAL_REQUEST_COUNT.set(0);
    }

    @AfterAll
    static void stopFinanceServer() {
        FINANCE_SERVER.disposeNow();
    }

    @Test
    void rejectsFinanceRouteWithoutAccessToken() {
        webTestClient
                .get()
                .uri(PRICING_PLANS_PATH)
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }

    @Test
    void forwardsFinanceRouteWithValidAccessToken() {
        String accessToken = createAccessToken();

        webTestClient
                .get()
                .uri(PRICING_PLANS_PATH)
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + accessToken
                )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.forwarded")
                .isEqualTo(true)
                .jsonPath("$.service")
                .isEqualTo("finance-service");
    }

    @Test
    void blocksFinanceInternalRouteAtGateway() {
        String accessToken = createAccessToken();

        String internalPath =
                INTERNAL_PREFIX
                        + "pricing-quotes/"
                        + UUID.randomUUID();

        webTestClient
                .get()
                .uri(internalPath)
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + accessToken
                )
                .exchange()
                .expectStatus()
                .isForbidden();

        assertThat(INTERNAL_REQUEST_COUNT.get())
                .isZero();
    }

    private String createAccessToken() {
        JwtEncoder encoder = NimbusJwtEncoder
                .withSecretKey(secretKey)
                .algorithm(MacAlgorithm.HS256)
                .build();

        Instant issuedAt = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .subject(
                        UUID.randomUUID().toString()
                )
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
                        "finance.gateway@centerflow.com"
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
    startFinanceServer() {
        return HttpServer.create()
                .host("127.0.0.1")
                .port(0)
                .route(routes ->
                        routes
                                .get(
                                        PRICING_PLANS_PATH,
                                        (request, response) ->
                                                jsonResponse(
                                                        response,
                                                        """
                                                        {
                                                          "forwarded": true,
                                                          "service": "finance-service"
                                                        }
                                                        """
                                                )
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