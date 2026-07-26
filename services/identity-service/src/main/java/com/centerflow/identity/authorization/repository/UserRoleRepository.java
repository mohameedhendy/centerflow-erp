package com.centerflow.identity.authorization.repository;

import com.centerflow.identity.authorization.domain.UserRole;
import com.centerflow.identity.authorization.domain.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserRoleRepository
        extends JpaRepository<UserRole, UserRoleId> {

    List<UserRole> findAllByIdUserId(UUID userId);

    boolean existsByIdUserIdAndIdRoleId(
            UUID userId,
            UUID roleId
    );
}