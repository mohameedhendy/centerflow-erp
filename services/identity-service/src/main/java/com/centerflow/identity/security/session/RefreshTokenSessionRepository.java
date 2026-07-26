package com.centerflow.identity.security.session;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenSessionRepository
        extends JpaRepository<RefreshTokenSession, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT session
            FROM RefreshTokenSession session
            WHERE session.tokenHash = :tokenHash
            """)
    Optional<RefreshTokenSession>
    findByTokenHashForUpdate(
            @Param("tokenHash") String tokenHash
    );
}