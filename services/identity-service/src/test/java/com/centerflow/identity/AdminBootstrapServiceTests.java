package com.centerflow.identity;

import com.centerflow.identity.authorization.bootstrap.AdminBootstrapResult;
import com.centerflow.identity.authorization.bootstrap.AdminBootstrapService;
import com.centerflow.identity.authorization.domain.RoleName;
import com.centerflow.identity.authorization.repository.RoleRepository;
import com.centerflow.identity.authorization.repository.UserRoleRepository;
import com.centerflow.identity.user.domain.User;
import com.centerflow.identity.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class AdminBootstrapServiceTests {

    private final AdminBootstrapService
            bootstrapService;

    private final UserRepository
            userRepository;

    private final RoleRepository
            roleRepository;

    private final UserRoleRepository
            userRoleRepository;

    private final PasswordEncoder
            passwordEncoder;

    @Autowired
    AdminBootstrapServiceTests(
            AdminBootstrapService bootstrapService,
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.bootstrapService =
                bootstrapService;

        this.userRepository =
                userRepository;

        this.roleRepository =
                roleRepository;

        this.userRoleRepository =
                userRoleRepository;

        this.passwordEncoder =
                passwordEncoder;
    }

    @Test
    void createsInitialAdministrator() {
        AdminBootstrapResult result =
                bootstrapService.bootstrap(
                        "initial.admin@centerflow.com",
                        "SecureAdmin123"
                );

        assertThat(result)
                .isEqualTo(
                        AdminBootstrapResult.CREATED
                );

        User administrator =
                userRepository
                        .findByEmailIgnoreCase(
                                "initial.admin@centerflow.com"
                        )
                        .orElseThrow();

        assertThat(administrator.isActive())
                .isTrue();

        assertThat(administrator.isEmailVerified())
                .isTrue();

        assertThat(
                passwordEncoder.matches(
                        "SecureAdmin123",
                        administrator.getPasswordHash()
                )
        ).isTrue();

        assertThat(
                roleRepository.findRoleNamesByUserId(
                        administrator.getId()
                )
        ).containsExactly(RoleName.ADMIN);
    }

    @Test
    void bootstrapIsIdempotentAfterAdministratorExists() {
        AdminBootstrapResult firstResult =
                bootstrapService.bootstrap(
                        "idempotent.admin@centerflow.com",
                        "SecureAdmin123"
                );

        AdminBootstrapResult secondResult =
                bootstrapService.bootstrap(
                        "another.admin@centerflow.com",
                        "AnotherAdmin123"
                );

        assertThat(firstResult)
                .isEqualTo(
                        AdminBootstrapResult.CREATED
                );

        assertThat(secondResult)
                .isEqualTo(
                        AdminBootstrapResult
                                .ALREADY_CONFIGURED
                );

        assertThat(
                userRepository.existsByEmailIgnoreCase(
                        "another.admin@centerflow.com"
                )
        ).isFalse();
    }

    @Test
    void refusesToPromoteExistingAccountDuringBootstrap() {
        User existingUser =
                User.createActive(
                        "existing.user@centerflow.com",
                        passwordEncoder.encode(
                                "ExistingUser123"
                        ),
                        Instant.now()
                );

        userRepository.saveAndFlush(
                existingUser
        );

        assertThatThrownBy(
                () ->
                        bootstrapService.bootstrap(
                                "existing.user@centerflow.com",
                                "SecureAdmin123"
                        )
        )
                .isInstanceOf(
                        IllegalStateException.class
                )
                .hasMessage(
                        "Bootstrap administrator email already belongs to an existing user"
                );

        assertThat(
                userRoleRepository
                        .findAllByIdUserId(
                                existingUser.getId()
                        )
        ).isEmpty();
    }
}