package com.centerflow.identity.auth.application;

import com.centerflow.identity.authorization.domain.Role;
import com.centerflow.identity.authorization.domain.RoleName;
import com.centerflow.identity.authorization.domain.UserRole;
import com.centerflow.identity.authorization.repository.RoleRepository;
import com.centerflow.identity.authorization.repository.UserRoleRepository;
import com.centerflow.identity.common.exception.DuplicateEmailException;
import com.centerflow.identity.common.exception.RequiredRoleNotFoundException;
import com.centerflow.identity.user.domain.User;
import com.centerflow.identity.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

@Service
public class UserRegistrationService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public UserRegistrationService(
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
    public UserRegistrationResult registerStudent(
            String email,
            String rawPassword
    ) {
        String normalizedEmail = normalizeEmail(email);

        if (userRepository.existsByEmailIgnoreCase(
                normalizedEmail
        )) {
            throw new DuplicateEmailException(normalizedEmail);
        }

        Role studentRole = roleRepository
                .findByName(RoleName.STUDENT)
                .orElseThrow(
                        () -> new RequiredRoleNotFoundException(
                                RoleName.STUDENT
                        )
                );

        Instant registeredAt = Instant.now(clock);
        String passwordHash = passwordEncoder.encode(rawPassword);

        User savedUser;

        try {
            User user = User.register(
                    normalizedEmail,
                    passwordHash,
                    registeredAt
            );

            savedUser = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateEmailException(
                    normalizedEmail,
                    exception
            );
        }

        userRoleRepository.save(
                UserRole.assign(
                        savedUser.getId(),
                        studentRole.getId(),
                        null,
                        registeredAt
                )
        );

        return new UserRegistrationResult(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getStatus(),
                savedUser.isEmailVerified(),
                studentRole.getName(),
                savedUser.getCreatedAt()
        );
    }

    private String normalizeEmail(String email) {
        return email
                .strip()
                .toLowerCase(Locale.ROOT);
    }
}