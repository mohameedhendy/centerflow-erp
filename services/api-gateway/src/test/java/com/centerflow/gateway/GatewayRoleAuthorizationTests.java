package com.centerflow.gateway;

import com.centerflow.gateway.security.GatewayJwtProperties;
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
import org.springframework.test.web.reactive.server.WebTestClient;

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
class GatewayRoleAuthorizationTests {

    private final WebTestClient webTestClient;
    private final SecretKey secretKey;
    private final GatewayJwtProperties jwtProperties;

    @Autowired
    GatewayRoleAuthorizationTests(
            WebTestClient webTestClient,
            SecretKey secretKey,
            GatewayJwtProperties jwtProperties
    ) {
        this.webTestClient = webTestClient;
        this.secretKey = secretKey;
        this.jwtProperties = jwtProperties;
    }

    @Test
    void studentCannotCreateAcademicBranch() {
        String token = createAccessToken("STUDENT");

        webTestClient
                .post()
                .uri("/api/v1/academic/branches")
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + token
                )
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(
                        """
                        {
                          "code": "RBAC-TEST",
                          "name": "RBAC Test Branch",
                          "city": "Cairo"
                        }
                        """
                )
                .exchange()
                .expectStatus()
                .isForbidden();
    }

    @Test
    void receptionistCannotModifyAcademicConfiguration() {
        String token = createAccessToken(
                "RECEPTIONIST"
        );

        webTestClient
                .post()
                .uri("/api/v1/academic/courses")
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + token
                )
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(
                        """
                        {
                          "code": "RBAC-COURSE",
                          "name": "RBAC Course"
                        }
                        """
                )
                .exchange()
                .expectStatus()
                .isForbidden();
    }

    @Test
    void studentCannotAccessEnrollmentManagement() {
        String token = createAccessToken("STUDENT");

        webTestClient
                .get()
                .uri("/api/v1/enrollments")
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + token
                )
                .exchange()
                .expectStatus()
                .isForbidden();
    }

    @Test
    void accountantCannotAccessEnrollmentManagement() {
        String token = createAccessToken(
                "ACCOUNTANT"
        );

        webTestClient
                .get()
                .uri("/api/v1/enrollments")
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + token
                )
                .exchange()
                .expectStatus()
                .isForbidden();
    }

    @Test
    void studentCannotAccessFinanceManagement() {
        String token = createAccessToken("STUDENT");

        webTestClient
                .get()
                .uri("/api/v1/finance/pricing-plans")
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + token
                )
                .exchange()
                .expectStatus()
                .isForbidden();
    }

    @Test
    void branchManagerCannotCreateFinancialTransaction() {
        String token = createAccessToken(
                "BRANCH_MANAGER"
        );

        UUID enrollmentId = UUID.randomUUID();

        webTestClient
                .post()
                .uri(
                        "/api/v1/finance/"
                                + "enrollment-accounts/"
                                + enrollmentId
                                + "/payments"
                )
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + token
                )
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(
                        """
                        {
                          "amount": 100.00,
                          "method": "CASH",
                          "externalReference": "RBAC-TEST"
                        }
                        """
                )
                .exchange()
                .expectStatus()
                .isForbidden();
    }

    @Test
    void studentCannotAccessIdentityAdministration() {
        String token = createAccessToken("STUDENT");

        UUID targetUserId = UUID.randomUUID();

        webTestClient
                .put()
                .uri(
                        "/api/v1/auth/admin/users/"
                                + targetUserId
                                + "/roles"
                )
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + token
                )
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(
                        """
                        {
                          "roles": [
                            "ACCOUNTANT"
                          ]
                        }
                        """
                )
                .exchange()
                .expectStatus()
                .isForbidden();
    }

    private String createAccessToken(
            String role
    ) {
        JwtEncoder encoder =
                NimbusJwtEncoder
                        .withSecretKey(secretKey)
                        .algorithm(MacAlgorithm.HS256)
                        .build();

        Instant issuedAt = Instant.now();

        JwtClaimsSet claims =
                JwtClaimsSet.builder()
                        .issuer(
                                jwtProperties.issuer()
                        )
                        .subject(
                                UUID.randomUUID()
                                        .toString()
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
                                "rbac.test@centerflow.com"
                        )
                        .claim(
                                "roles",
                                List.of(role)
                        )
                        .build();

        JwsHeader header =
                JwsHeader
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
}