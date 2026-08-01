package com.centerflow.identity.authorization.repository;

import com.centerflow.identity.authorization.domain.Role;
import com.centerflow.identity.authorization.domain.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository
        extends JpaRepository<Role, UUID> {

    Optional<Role> findByName(RoleName name);

    boolean existsByName(RoleName name);

    List<Role> findAllByNameIn(
            Collection<RoleName> names
    );

    @Query("""
            SELECT role.name
            FROM Role role
            WHERE role.id IN (
                SELECT userRole.id.roleId
                FROM UserRole userRole
                WHERE userRole.id.userId = :userId
            )
            ORDER BY role.name
            """)
    List<RoleName> findRoleNamesByUserId(
            @Param("userId") UUID userId
    );
}