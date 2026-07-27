package com.centerflow.academic.batchsession.repository;

import com.centerflow.academic.batchsession.domain.BatchSession;
import com.centerflow.academic.batchsession.domain.BatchSessionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public interface BatchSessionRepository
        extends JpaRepository<BatchSession, UUID> {

    boolean existsByBatchIdAndSessionDateAndStartTimeAndEndTime(
            UUID batchId,
            LocalDate sessionDate,
            LocalTime startTime,
            LocalTime endTime
    );

    @Query("""
            SELECT session
            FROM BatchSession session
            WHERE session.batchId = :batchId
            AND (
                :dateFrom IS NULL
                OR session.sessionDate >= :dateFrom
            )
            AND (
                :dateTo IS NULL
                OR session.sessionDate <= :dateTo
            )
            AND (
                :status IS NULL
                OR session.status = :status
            )
            """)
    Page<BatchSession> search(
            @Param("batchId") UUID batchId,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            @Param("status")
            BatchSessionStatus status,
            Pageable pageable
    );

    @Query(
            value = """
                    SELECT COUNT(*)
                    FROM batch_sessions session
                    JOIN batches existing_batch
                      ON existing_batch.id = session.batch_id
                    WHERE session.status <> 'CANCELLED'
                      AND existing_batch.classroom_id = :classroomId
                      AND session.session_date = :sessionDate
                      AND session.start_time < :endTime
                      AND session.end_time > :startTime
                      AND (
                          :excludedSessionId IS NULL
                          OR session.id <> :excludedSessionId
                      )
                    """,
            nativeQuery = true
    )
    long countClassroomConflicts(
            @Param("classroomId") UUID classroomId,
            @Param("sessionDate") LocalDate sessionDate,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("excludedSessionId")
            UUID excludedSessionId
    );

    @Query(
            value = """
                    SELECT COUNT(*)
                    FROM batch_sessions session
                    JOIN batches existing_batch
                      ON existing_batch.id = session.batch_id
                    WHERE session.status <> 'CANCELLED'
                      AND existing_batch.instructor_id = :instructorId
                      AND session.session_date = :sessionDate
                      AND session.start_time < :endTime
                      AND session.end_time > :startTime
                      AND (
                          :excludedSessionId IS NULL
                          OR session.id <> :excludedSessionId
                      )
                    """,
            nativeQuery = true
    )
    long countInstructorConflicts(
            @Param("instructorId") UUID instructorId,
            @Param("sessionDate") LocalDate sessionDate,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("excludedSessionId")
            UUID excludedSessionId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT session
        FROM BatchSession session
        WHERE session.id = :sessionId
        """)
    Optional<BatchSession> findByIdForUpdate(
            @Param("sessionId") UUID sessionId
    );
}