package com.centerflow.identity;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PasswordResetApiTests {

    private static final String REGISTER_URL =
            "/api/v1/auth/register";

    private static final String LOGIN_URL =
            "/api/v1/auth/login";

    private static final String REFRESH_URL =
            "/api/v1/auth/refresh";

    private static final String FORGOT_URL =
            "/api/v1/auth/password/forgot";

    private static final String RESET_URL =
            "/api/v1/auth/password/reset";

    private final MockMvc mockMvc;

    @Autowired
    PasswordResetApiTests(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void resetsPasswordAndRevokesRefreshSessions()
            throws Exception {

        String email =
                "reset.student@centerflow.com";

        registerUser(email, "Student123");

        LoginTokens oldTokens =
                login(email, "Student123");

        String resetToken =
                requestResetToken(email);

        mockMvc.perform(
                        post(RESET_URL)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        resetRequest(
                                                resetToken,
                                                "NewStudent123"
                                        )
                                )
                )
                .andExpect(status().isNoContent());

        mockMvc.perform(
                        post(LOGIN_URL)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        loginRequest(
                                                email,
                                                "Student123"
                                        )
                                )
                )
                .andExpect(status().isUnauthorized());

        mockMvc.perform(
                        post(LOGIN_URL)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        loginRequest(
                                                email,
                                                "NewStudent123"
                                        )
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.accessToken")
                                .isNotEmpty()
                );

        mockMvc.perform(
                        post(REFRESH_URL)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        refreshRequest(
                                                oldTokens.refreshToken()
                                        )
                                )
                )
                .andExpect(status().isUnauthorized());

        mockMvc.perform(
                        post(RESET_URL)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        resetRequest(
                                                resetToken,
                                                "AnotherStudent123"
                                        )
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Invalid or expired password reset token"
                                )
                );
    }

    @Test
    void secondResetRequestRevokesFirstToken()
            throws Exception {

        String email =
                "second.reset@centerflow.com";

        registerUser(email, "Student123");

        String firstToken =
                requestResetToken(email);

        String secondToken =
                requestResetToken(email);

        assertThat(secondToken)
                .isNotEqualTo(firstToken);

        mockMvc.perform(
                        post(RESET_URL)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        resetRequest(
                                                firstToken,
                                                "NewStudent123"
                                        )
                                )
                )
                .andExpect(status().isBadRequest());

        mockMvc.perform(
                        post(RESET_URL)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        resetRequest(
                                                secondToken,
                                                "NewStudent123"
                                        )
                                )
                )
                .andExpect(status().isNoContent());
    }

    @Test
    void forgotPasswordDoesNotRevealMissingAccount()
            throws Exception {

        mockMvc.perform(
                        post(FORGOT_URL)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "email": "missing@centerflow.com"
                                        }
                                        """
                                )
                )
                .andExpect(status().isAccepted())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "If an active account exists for this email, password reset instructions have been created"
                                )
                )
                .andExpect(
                        jsonPath("$.resetToken")
                                .doesNotExist()
                );
    }

    private void registerUser(
            String email,
            String password
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
                                          "password": "%s"
                                        }
                                        """.formatted(
                                                email,
                                                password
                                        )
                                )
                )
                .andExpect(status().isCreated());
    }

    private LoginTokens login(
            String email,
            String password
    ) throws Exception {

        MvcResult result = mockMvc.perform(
                        post(LOGIN_URL)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        loginRequest(
                                                email,
                                                password
                                        )
                                )
                )
                .andExpect(status().isOk())
                .andReturn();

        String responseBody =
                result.getResponse()
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

    private String requestResetToken(
            String email
    ) throws Exception {

        MvcResult result = mockMvc.perform(
                        post(FORGOT_URL)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "email": "%s"
                                        }
                                        """.formatted(email)
                                )
                )
                .andExpect(status().isAccepted())
                .andExpect(
                        jsonPath("$.resetToken")
                                .isNotEmpty()
                )
                .andReturn();

        return JsonPath.read(
                result.getResponse()
                        .getContentAsString(),
                "$.resetToken"
        );
    }

    private String loginRequest(
            String email,
            String password
    ) {
        return """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, password);
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

    private String resetRequest(
            String resetToken,
            String newPassword
    ) {
        return """
                {
                  "resetToken": "%s",
                  "newPassword": "%s"
                }
                """.formatted(
                resetToken,
                newPassword
        );
    }

    private record LoginTokens(
            String accessToken,
            String refreshToken
    ) {
    }
}