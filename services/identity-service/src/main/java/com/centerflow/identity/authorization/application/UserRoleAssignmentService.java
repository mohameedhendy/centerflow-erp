package com.centerflow.identity.authorization.application;

import com.centerflow.identity.authorization.domain.Role;
import com.centerflow.identity.authorization.domain.RoleName;
import com.centerflow.identity.authorization.domain.UserRole;
import com.centerflow.identity.authorization.repository.RoleRepository;
import com.centerflow.identity.authorization.repository.UserRoleRepository;
import com.centerflow.identity.common.exception.CannotRemoveOwnAdminRoleException;
import com.centerflow.identity.common.exception.IdentityUserNotFoundException;
import com.centerflow.identity.common.exception.RequiredRoleNotFoundException;
import com.centerflow.identity.user.domain.User;
import com.centerflow.identity.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserRoleAssignmentService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final Clock clock;

    public UserRoleAssignmentService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.clock = clock;
    }

    @Transactional
    public UserRoleAssignmentResult replaceRoles(
            UUID targetUserId,
            Set<RoleName> requestedRoles,
            UUID assignedBy
    ) {
        Objects.requireNonNull(
                targetUserId,
                "Target user ID is required"
        );

        Objects.requireNonNull(
                assignedBy,
                "Assigning user ID is required"
        );

        if (requestedRoles == null
                || requestedRoles.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one role is required"
            );
        }

        User targetUser = userRepository
                .findById(targetUserId)
                .orElseThrow(
                        () ->
                                new IdentityUserNotFoundException(
                                        targetUserId
                                )
                );

        userRepository
                .findById(assignedBy)
                .orElseThrow(
                        () ->
                                new IdentityUserNotFoundException(
                                        assignedBy
                                )
                );

        Set<RoleName> desiredRoleNames =
                Set.copyOf(requestedRoles);

        if (targetUserId.equals(assignedBy)
                && !desiredRoleNames.contains(
                RoleName.ADMIN
        )) {
            throw new CannotRemoveOwnAdminRoleException();
        }

        List<Role> desiredRoles =
                roleRepository.findAllByNameIn(
                        desiredRoleNames
                );

        Set<RoleName> configuredRoleNames =
                desiredRoles.stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet());

        desiredRoleNames.stream()
                .filter(
                        roleName ->
                                !configuredRoleNames.contains(
                                        roleName
                                )
                )
                .findFirst()
                .ifPresent(
                        roleName -> {
                            throw new RequiredRoleNotFoundException(
                                    roleName
                            );
                        }
                );

        List<UserRole> currentAssignments =
                userRoleRepository
                        .findAllByIdUserId(
                                targetUserId
                        );

        Set<UUID> desiredRoleIds =
                desiredRoles.stream()
                        .map(Role::getId)
                        .collect(Collectors.toSet());

        List<UserRole> removedAssignments =
                currentAssignments.stream()
                        .filter(
                                assignment ->
                                        !desiredRoleIds.contains(
                                                assignment
                                                        .getId()
                                                        .getRoleId()
                                        )
                        )
                        .toList();

        if (!removedAssignments.isEmpty()) {
            userRoleRepository.deleteAll(
                    removedAssignments
            );
        }

        Set<UUID> currentlyAssignedRoleIds =
                currentAssignments.stream()
                        .map(
                                assignment ->
                                        assignment
                                                .getId()
                                                .getRoleId()
                        )
                        .collect(Collectors.toSet());

        Instant assignedAt = Instant.now(clock);

        List<UserRole> newAssignments =
                desiredRoles.stream()
                        .filter(
                                role ->
                                        !currentlyAssignedRoleIds
                                                .contains(
                                                        role.getId()
                                                )
                        )
                        .map(
                                role ->
                                        UserRole.assign(
                                                targetUserId,
                                                role.getId(),
                                                assignedBy,
                                                assignedAt
                                        )
                        )
                        .toList();

        if (!newAssignments.isEmpty()) {
            userRoleRepository.saveAll(
                    newAssignments
            );
        }

        userRoleRepository.flush();

        List<RoleName> responseRoles =
                desiredRoles.stream()
                        .map(Role::getName)
                        .sorted()
                        .toList();

        return new UserRoleAssignmentResult(
                targetUser.getId(),
                targetUser.getEmail(),
                responseRoles,
                assignedBy,
                assignedAt
        );
    }
}