package com.centerflow.identity;

import com.centerflow.identity.authorization.domain.Role;
import com.centerflow.identity.authorization.domain.RoleName;
import com.centerflow.identity.authorization.domain.UserRole;
import com.centerflow.identity.authorization.repository.RoleRepository;
import com.centerflow.identity.authorization.repository.UserRoleRepository;
import com.centerflow.identity.security.config.JwtProperties;
import com.centerflow.identity.user.domain.User;
import com.centerflow.identity.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminUserRoleApiTests {

    private final MockMvc mockMvc;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;

    @Autowired
    AdminUserRoleApiTests(
            MockMvc mockMvc,
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            PasswordEncoder passwordEncoder,
            JwtEncoder jwtEncoder,
            JwtProperties jwtProperties
    ) {
        this.mockMvc = mockMvc;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.jwtProperties = jwtProperties;
    }

    @Test
    void adminCanReplaceUserRoles()
            throws Exception {
        User admin = createActiveUser(
                "role.admin@centerflow.com"
        );

        User target = createActiveUser(
                "role.target@centerflow.com"
        );

        assignRole(
                admin,
                RoleName.ADMIN,
                admin.getId()
        );

        assignRole(
                target,
                RoleName.STUDENT,
                admin.getId()
        );

        String token = createAccessToken(
                admin,
                RoleName.ADMIN
        );

        String url =
                "/api/v1/auth/admin/users/"
                        + target.getId()
                        + "/roles";

        mockMvc.perform(
                        put(url)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "roles": [
                                            "ACCOUNTANT",
                                            "RECEPTIONIST"
                                          ]
                                        }
                                        """
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.userId")
                                .value(
                                        target.getId()
                                                .toString()
                                )
                )
                .andExpect(
                        jsonPath("$.email")
                                .value(
                                        target.getEmail()
                                )
                )
                .andExpect(
                        jsonPath("$.roles[0]")
                                .value("ACCOUNTANT")
                )
                .andExpect(
                        jsonPath("$.roles[1]")
                                .value("RECEPTIONIST")
                )
                .andExpect(
                        jsonPath("$.assignedBy")
                                .value(
                                        admin.getId()
                                                .toString()
                                )
                )
                .andExpect(
                        jsonPath("$.assignedAt")
                                .isNotEmpty()
                );

        assertThat(
                roleRepository.findRoleNamesByUserId(
                        target.getId()
                )
        ).containsExactly(
                RoleName.ACCOUNTANT,
                RoleName.RECEPTIONIST
        );

        List<UserRole> assignments =
                userRoleRepository
                        .findAllByIdUserId(
                                target.getId()
                        );

        assertThat(assignments)
                .hasSize(2)
                .allSatisfy(
                        assignment ->
                                assertThat(
                                        assignment
                                                .getAssignedBy()
                                ).isEqualTo(
                                        admin.getId()
                                )
                );
    }

    @Test
    void studentCannotAssignRoles()
            throws Exception {
        User student = createActiveUser(
                "role.student@centerflow.com"
        );

        User target = createActiveUser(
                "forbidden.target@centerflow.com"
        );

        assignRole(
                student,
                RoleName.STUDENT,
                null
        );

        String token = createAccessToken(
                student,
                RoleName.STUDENT
        );

        mockMvc.perform(
                        put(
                                "/api/v1/auth/admin/users/"
                                        + target.getId()
                                        + "/roles"
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "roles": [
                                            "ACCOUNTANT"
                                          ]
                                        }
                                        """
                                )
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCannotRemoveOwnAdminRole()
            throws Exception {
        User admin = createActiveUser(
                "self.admin@centerflow.com"
        );

        assignRole(
                admin,
                RoleName.ADMIN,
                admin.getId()
        );

        String token = createAccessToken(
                admin,
                RoleName.ADMIN
        );

        mockMvc.perform(
                        put(
                                "/api/v1/auth/admin/users/"
                                        + admin.getId()
                                        + "/roles"
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "roles": [
                                            "ACCOUNTANT"
                                          ]
                                        }
                                        """
                                )
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.status")
                                .value(409)
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "An administrator cannot remove their own ADMIN role"
                                )
                );

        assertThat(
                roleRepository.findRoleNamesByUserId(
                        admin.getId()
                )
        ).containsExactly(RoleName.ADMIN);
    }

    @Test
    void adminReceivesNotFoundForUnknownUser()
            throws Exception {
        User admin = createActiveUser(
                "missing.admin@centerflow.com"
        );

        assignRole(
                admin,
                RoleName.ADMIN,
                admin.getId()
        );

        String token = createAccessToken(
                admin,
                RoleName.ADMIN
        );

        UUID missingUserId = UUID.randomUUID();

        mockMvc.perform(
                        put(
                                "/api/v1/auth/admin/users/"
                                        + missingUserId
                                        + "/roles"
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "roles": [
                                            "RECEPTIONIST"
                                          ]
                                        }
                                        """
                                )
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.status")
                                .value(404)
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Identity user was not found: "
                                                + missingUserId
                                )
                );
    }

    @Test
    void adminCannotAssignEmptyRoleSet()
            throws Exception {
        User admin = createActiveUser(
                "validation.admin@centerflow.com"
        );

        User target = createActiveUser(
                "validation.target@centerflow.com"
        );

        assignRole(
                admin,
                RoleName.ADMIN,
                admin.getId()
        );

        String token = createAccessToken(
                admin,
                RoleName.ADMIN
        );

        mockMvc.perform(
                        put(
                                "/api/v1/auth/admin/users/"
                                        + target.getId()
                                        + "/roles"
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "roles": []
                                        }
                                        """
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath(
                                "$.validationErrors.roles"
                        ).value(
                                "At least one role is required"
                        )
                );
    }

    private User createActiveUser(
            String email
    ) {
        User user = User.createActive(
                email,
                passwordEncoder.encode(
                        "Admin123"
                ),
                Instant.now()
        );

        return userRepository.saveAndFlush(user);
    }

    private void assignRole(
            User user,
            RoleName roleName,
            UUID assignedBy
    ) {
        Role role = roleRepository
                .findByName(roleName)
                .orElseThrow();

        userRoleRepository.saveAndFlush(
                UserRole.assign(
                        user.getId(),
                        role.getId(),
                        assignedBy,
                        Instant.now()
                )
        );
    }

    private String createAccessToken(
            User user,
            RoleName roleName
    ) {
        Instant issuedAt = Instant.now();

        JwtClaimsSet claims =
                JwtClaimsSet.builder()
                        .issuer(
                                jwtProperties.issuer()
                        )
                        .subject(
                                user.getId()
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
                                user.getEmail()
                        )
                        .claim(
                                "roles",
                                List.of(
                                        roleName.name()
                                )
                        )
                        .build();

        JwsHeader header =
                JwsHeader
                        .with(MacAlgorithm.HS256)
                        .type("JWT")
                        .build();

        return jwtEncoder
                .encode(
                        JwtEncoderParameters.from(
                                header,
                                claims
                        )
                )
                .getTokenValue();
    }
}