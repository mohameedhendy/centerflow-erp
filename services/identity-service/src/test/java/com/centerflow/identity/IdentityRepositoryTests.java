package com.centerflow.identity;

import com.centerflow.identity.authorization.domain.Permission;
import com.centerflow.identity.authorization.domain.Role;
import com.centerflow.identity.authorization.domain.RoleName;
import com.centerflow.identity.authorization.domain.RolePermission;
import com.centerflow.identity.authorization.domain.UserRole;
import com.centerflow.identity.authorization.repository.PermissionRepository;
import com.centerflow.identity.authorization.repository.RolePermissionRepository;
import com.centerflow.identity.authorization.repository.RoleRepository;
import com.centerflow.identity.authorization.repository.UserRoleRepository;
import com.centerflow.identity.user.domain.User;
import com.centerflow.identity.user.domain.UserStatus;
import com.centerflow.identity.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class IdentityRepositoryTests {

    private static final Instant TEST_TIME =
            Instant.parse("2026-07-26T12:00:00Z");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;

    @Autowired
    IdentityRepositoryTests(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            UserRoleRepository userRoleRepository,
            RolePermissionRepository rolePermissionRepository
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.userRoleRepository = userRoleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
    }

    @Test
    void savesAndFindsUserByEmailIgnoringCase() {
        User user = User.createPendingVerification(
                "  Student@CenterFlow.com  ",
                "test-password-hash",
                TEST_TIME
        );

        User savedUser =
                userRepository.saveAndFlush(user);

        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getEmail())
                .isEqualTo("student@centerflow.com");
        assertThat(savedUser.getStatus())
                .isEqualTo(
                        UserStatus.PENDING_VERIFICATION
                );
        assertThat(savedUser.isEmailVerified()).isFalse();

        assertThat(
                userRepository.findByEmailIgnoreCase(
                        "STUDENT@CENTERFLOW.COM"
                )
        )
                .isPresent()
                .contains(savedUser);

        assertThat(
                userRepository.existsByEmailIgnoreCase(
                        "Student@CenterFlow.com"
                )
        ).isTrue();
    }

    @Test
    void storesRoleAndPermissionAssignments() {
        User user = userRepository.saveAndFlush(
                User.createActive(
                        "admin@centerflow.com",
                        "test-password-hash",
                        TEST_TIME
                )
        );

        Role role = roleRepository
                .findByName(RoleName.ADMIN)
                .orElseThrow();

        Permission permission =
                permissionRepository.saveAndFlush(
                        Permission.create(
                                "user_create",
                                "Create identity users",
                                TEST_TIME
                        )
                );

        userRoleRepository.saveAndFlush(
                UserRole.assign(
                        user.getId(),
                        role.getId(),
                        null,
                        TEST_TIME
                )
        );

        rolePermissionRepository.saveAndFlush(
                RolePermission.assign(
                        role.getId(),
                        permission.getId(),
                        null,
                        TEST_TIME
                )
        );

        assertThat(
                permissionRepository.findByName(
                        "USER_CREATE"
                )
        )
                .isPresent()
                .contains(permission);

        assertThat(
                userRoleRepository
                        .existsByIdUserIdAndIdRoleId(
                                user.getId(),
                                role.getId()
                        )
        ).isTrue();

        assertThat(
                rolePermissionRepository
                        .existsByIdRoleIdAndIdPermissionId(
                                role.getId(),
                                permission.getId()
                        )
        ).isTrue();

        assertThat(
                userRoleRepository.findAllByIdUserId(
                        user.getId()
                )
        ).hasSize(1);

        assertThat(
                rolePermissionRepository.findAllByIdRoleId(
                        role.getId()
                )
        ).hasSize(1);
    }
}