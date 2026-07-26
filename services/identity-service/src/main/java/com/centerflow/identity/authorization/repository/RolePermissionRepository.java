package com.centerflow.identity.authorization.repository;

import com.centerflow.identity.authorization.domain.RolePermission;
import com.centerflow.identity.authorization.domain.RolePermissionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RolePermissionRepository
        extends JpaRepository<RolePermission, RolePermissionId> {

    List<RolePermission> findAllByIdRoleId(UUID roleId);

    boolean existsByIdRoleIdAndIdPermissionId(
            UUID roleId,
            UUID permissionId
    );
}