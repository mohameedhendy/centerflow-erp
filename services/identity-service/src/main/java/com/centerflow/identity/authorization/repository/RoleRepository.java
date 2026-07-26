package com.centerflow.identity.authorization.repository;

import com.centerflow.identity.authorization.domain.Role;
import com.centerflow.identity.authorization.domain.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByName(RoleName name);

    boolean existsByName(RoleName name);
}