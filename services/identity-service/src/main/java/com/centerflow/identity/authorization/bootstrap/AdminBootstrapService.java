package com.centerflow.identity.authorization.bootstrap;

import com.centerflow.identity.authorization.domain.Role;
import com.centerflow.identity.authorization.domain.RoleName;
import com.centerflow.identity.authorization.domain.UserRole;
import com.centerflow.identity.authorization.repository.RoleRepository;
import com.centerflow.identity.authorization.repository.UserRoleRepository;
import com.centerflow.identity.common.exception.RequiredRoleNotFoundException;
import com.centerflow.identity.user.domain.User;
import com.centerflow.identity.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class AdminBootstrapService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public AdminBootstrapService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            PasswordEncoder passwordEncoder,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional
    public AdminBootstrapResult bootstrap(
            String email,
            String rawPassword
    ) {
        Role adminRole =
                roleRepository
                        .findByName(RoleName.ADMIN)
                        .orElseThrow(
                                () ->
                                        new RequiredRoleNotFoundException(
                                                RoleName.ADMIN
                                        )
                        );

        boolean administratorAlreadyExists =
                userRoleRepository.existsByIdRoleId(
                        adminRole.getId()
                );

        if (administratorAlreadyExists) {
            return AdminBootstrapResult
                    .ALREADY_CONFIGURED;
        }

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalStateException(
                    "Bootstrap administrator email already belongs to an existing user"
            );
        }

        Instant createdAt = Instant.now(clock);

        User administrator =
                User.createActive(
                        email,
                        passwordEncoder.encode(
                                rawPassword
                        ),
                        createdAt
                );

        administrator =
                userRepository.saveAndFlush(
                        administrator
                );

        UserRole administratorRole =
                UserRole.assign(
                        administrator.getId(),
                        adminRole.getId(),
                        administrator.getId(),
                        createdAt
                );

        userRoleRepository.saveAndFlush(
                administratorRole
        );

        return AdminBootstrapResult.CREATED;
    }
}