package com.centerflow.identity;

import com.centerflow.identity.authorization.domain.Role;
import com.centerflow.identity.authorization.domain.RoleName;
import com.centerflow.identity.authorization.repository.RoleRepository;
import com.centerflow.identity.authorization.repository.UserRoleRepository;
import com.centerflow.identity.user.domain.User;
import com.centerflow.identity.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserRegistrationApiTests {

    private static final String REGISTER_URL =
            "/api/v1/auth/register";

    private final MockMvc mockMvc;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    UserRegistrationApiTests(
            MockMvc mockMvc,
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.mockMvc = mockMvc;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Test
    void registersStudentAndAssignsDefaultRole()
            throws Exception {
        String rawPassword = "Student123";

        mockMvc.perform(
                        post(REGISTER_URL)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "email": "Student@CenterFlow.com",
                                          "password": "Student123"
                                        }
                                        """
                                )
                )
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.id").isNotEmpty()
                )
                .andExpect(
                        jsonPath("$.email")
                                .value(
                                        "student@centerflow.com"
                                )
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("ACTIVE")
                )
                .andExpect(
                        jsonPath("$.emailVerified")
                                .value(false)
                )
                .andExpect(
                        jsonPath("$.role")
                                .value("STUDENT")
                );

        User savedUser = userRepository
                .findByEmailIgnoreCase(
                        "student@centerflow.com"
                )
                .orElseThrow();

        assertThat(savedUser.getPasswordHash())
                .isNotEqualTo(rawPassword);

        assertThat(
                passwordEncoder.matches(
                        rawPassword,
                        savedUser.getPasswordHash()
                )
        ).isTrue();

        Role studentRole = roleRepository
                .findByName(RoleName.STUDENT)
                .orElseThrow();

        assertThat(
                userRoleRepository
                        .existsByIdUserIdAndIdRoleId(
                                savedUser.getId(),
                                studentRole.getId()
                        )
        ).isTrue();
    }

    @Test
    void rejectsDuplicateEmailIgnoringCase()
            throws Exception {
        mockMvc.perform(
                        post(REGISTER_URL)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "email": "duplicate@centerflow.com",
                                          "password": "Student123"
                                        }
                                        """
                                )
                )
                .andExpect(status().isCreated());

        mockMvc.perform(
                        post(REGISTER_URL)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "email": "DUPLICATE@CENTERFLOW.COM",
                                          "password": "Another123"
                                        }
                                        """
                                )
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.status").value(409)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value("Conflict")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "An account already exists for email: duplicate@centerflow.com"
                                )
                )
                .andExpect(
                        jsonPath("$.path")
                                .value(REGISTER_URL)
                );
    }

    @Test
    void rejectsInvalidRegistrationRequest()
            throws Exception {
        mockMvc.perform(
                        post(REGISTER_URL)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "email": "invalid-email",
                                          "password": "password"
                                        }
                                        """
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.status").value(400)
                )
                .andExpect(
                        jsonPath("$.message")
                                .value("Validation failed")
                )
                .andExpect(
                        jsonPath(
                                "$.validationErrors.email"
                        ).value(
                                "Email format is invalid"
                        )
                )
                .andExpect(
                        jsonPath(
                                "$.validationErrors.password"
                        ).value(
                                "Password must contain at least one letter and one number"
                        )
                );
    }
}