package com.centerflow.identity;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserLoginApiTests {

    private static final String REGISTER_URL =
            "/api/v1/auth/register";

    private static final String LOGIN_URL =
            "/api/v1/auth/login";

    private static final String CURRENT_USER_URL =
            "/api/v1/auth/me";

    private static final String EMAIL =
            "login.student@centerflow.com";

    private static final String PASSWORD =
            "Student123";

    private final MockMvc mockMvc;
    private final JwtDecoder jwtDecoder;

    @Autowired
    UserLoginApiTests(
            MockMvc mockMvc,
            JwtDecoder jwtDecoder
    ) {
        this.mockMvc = mockMvc;
        this.jwtDecoder = jwtDecoder;
    }

    @Test
    void loginReturnsValidAccessTokenAndAllowsAccess()
            throws Exception {
        registerUser();

        MvcResult result = mockMvc.perform(
                        post(LOGIN_URL)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "email": "LOGIN.STUDENT@CENTERFLOW.COM",
                                          "password": "Student123"
                                        }
                                        """
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.accessToken")
                                .isNotEmpty()
                )
                .andExpect(
                        jsonPath("$.tokenType")
                                .value("Bearer")
                )
                .andExpect(
                        jsonPath("$.expiresIn")
                                .value(900)
                )
                .andExpect(
                        jsonPath("$.email")
                                .value(EMAIL)
                )
                .andExpect(
                        jsonPath("$.roles[0]")
                                .value("STUDENT")
                )
                .andReturn();

        String responseBody =
                result.getResponse()
                        .getContentAsString();

        String accessToken =
                JsonPath.read(
                        responseBody,
                        "$.accessToken"
                );

        Jwt jwt = jwtDecoder.decode(accessToken);

        assertThat(jwt.getSubject()).isNotBlank();
        assertThat(
                jwt.getClaimAsString("iss")
        ).isEqualTo("centerflow-identity-test");

        assertThat(jwt.getAudience())
                .contains("centerflow-api-test");

        assertThat(
                jwt.getClaimAsString("email")
        ).isEqualTo(EMAIL);

        assertThat(
                jwt.getClaimAsStringList("roles")
        ).containsExactly("STUDENT");

        mockMvc.perform(
                        get(CURRENT_USER_URL)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + accessToken
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id").isNotEmpty()
                )
                .andExpect(
                        jsonPath("$.email")
                                .value(EMAIL)
                )
                .andExpect(
                        jsonPath("$.roles[0]")
                                .value("STUDENT")
                );
    }

    @Test
    void loginRejectsInvalidPassword()
            throws Exception {
        registerUser();

        mockMvc.perform(
                        post(LOGIN_URL)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "email": "login.student@centerflow.com",
                                          "password": "WrongPassword123"
                                        }
                                        """
                                )
                )
                .andExpect(status().isUnauthorized())
                .andExpect(
                        jsonPath("$.status")
                                .value(401)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value("Unauthorized")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Invalid email or password"
                                )
                )
                .andExpect(
                        jsonPath("$.path")
                                .value(LOGIN_URL)
                );
    }

    @Test
    void protectedEndpointRejectsMissingToken()
            throws Exception {
        mockMvc.perform(
                        get(CURRENT_USER_URL)
                )
                .andExpect(status().isUnauthorized());
    }

    private void registerUser()
            throws Exception {
        mockMvc.perform(
                        post(REGISTER_URL)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "email": "login.student@centerflow.com",
                                          "password": "Student123"
                                        }
                                        """
                                )
                )
                .andExpect(status().isCreated());
    }
}