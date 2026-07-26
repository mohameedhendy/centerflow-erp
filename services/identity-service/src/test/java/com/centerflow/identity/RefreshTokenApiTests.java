package com.centerflow.identity;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
class RefreshTokenApiTests {

    private static final String REGISTER_URL =
            "/api/v1/auth/register";

    private static final String LOGIN_URL =
            "/api/v1/auth/login";

    private static final String REFRESH_URL =
            "/api/v1/auth/refresh";

    private static final String LOGOUT_URL =
            "/api/v1/auth/logout";

    private static final String CURRENT_USER_URL =
            "/api/v1/auth/me";

    private final MockMvc mockMvc;

    @Autowired
    RefreshTokenApiTests(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void refreshRotatesTokenAndRejectsOldToken()
            throws Exception {

        LoginTokens initialTokens =
                registerAndLogin(
                        "refresh.student@centerflow.com"
                );

        MvcResult refreshResult =
                mockMvc.perform(
                                post(REFRESH_URL)
                                        .contentType(
                                                MediaType.APPLICATION_JSON
                                        )
                                        .content(
                                                refreshRequest(
                                                        initialTokens.refreshToken()
                                                )
                                        )
                        )
                        .andExpect(status().isOk())
                        .andExpect(
                                jsonPath("$.accessToken")
                                        .isNotEmpty()
                        )
                        .andExpect(
                                jsonPath("$.refreshToken")
                                        .isNotEmpty()
                        )
                        .andExpect(
                                jsonPath("$.expiresIn")
                                        .value(900)
                        )
                        .andExpect(
                                jsonPath("$.refreshTokenExpiresIn")
                                        .value(604800)
                        )
                        .andReturn();

        String responseBody =
                refreshResult.getResponse()
                        .getContentAsString();

        String newAccessToken =
                JsonPath.read(
                        responseBody,
                        "$.accessToken"
                );

        String newRefreshToken =
                JsonPath.read(
                        responseBody,
                        "$.refreshToken"
                );

        assertThat(newAccessToken)
                .isNotEqualTo(
                        initialTokens.accessToken()
                );

        assertThat(newRefreshToken)
                .isNotEqualTo(
                        initialTokens.refreshToken()
                );

        mockMvc.perform(
                        get(CURRENT_USER_URL)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + newAccessToken
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.email")
                                .value(
                                        "refresh.student@centerflow.com"
                                )
                );

        mockMvc.perform(
                        post(REFRESH_URL)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        refreshRequest(
                                                initialTokens.refreshToken()
                                        )
                                )
                )
                .andExpect(status().isUnauthorized())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Invalid or expired refresh token"
                                )
                );
    }

    @Test
    void logoutRevokesRefreshToken()
            throws Exception {

        LoginTokens tokens =
                registerAndLogin(
                        "logout.student@centerflow.com"
                );

        mockMvc.perform(
                        post(LOGOUT_URL)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        refreshRequest(
                                                tokens.refreshToken()
                                        )
                                )
                )
                .andExpect(status().isNoContent());

        mockMvc.perform(
                        post(REFRESH_URL)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        refreshRequest(
                                                tokens.refreshToken()
                                        )
                                )
                )
                .andExpect(status().isUnauthorized())
                .andExpect(
                        jsonPath("$.status")
                                .value(401)
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Invalid or expired refresh token"
                                )
                );
    }

    private LoginTokens registerAndLogin(
            String email
    ) throws Exception {

        mockMvc.perform(
                        post(REGISTER_URL)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "email": "%s",
                                          "password": "Student123"
                                        }
                                        """.formatted(email)
                                )
                )
                .andExpect(status().isCreated());

        MvcResult loginResult =
                mockMvc.perform(
                                post(LOGIN_URL)
                                        .contentType(
                                                MediaType.APPLICATION_JSON
                                        )
                                        .content(
                                                """
                                                {
                                                  "email": "%s",
                                                  "password": "Student123"
                                                }
                                                """.formatted(email)
                                        )
                        )
                        .andExpect(status().isOk())
                        .andReturn();

        String responseBody =
                loginResult.getResponse()
                        .getContentAsString();

        return new LoginTokens(
                JsonPath.read(
                        responseBody,
                        "$.accessToken"
                ),
                JsonPath.read(
                        responseBody,
                        "$.refreshToken"
                )
        );
    }

    private String refreshRequest(
            String refreshToken
    ) {
        return """
                {
                  "refreshToken": "%s"
                }
                """.formatted(refreshToken);
    }

    private record LoginTokens(
            String accessToken,
            String refreshToken
    ) {
    }
}