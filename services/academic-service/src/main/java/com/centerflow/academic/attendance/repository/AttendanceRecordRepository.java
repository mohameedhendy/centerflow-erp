package com.centerflow.academic.attendance.repository;

import com.centerflow.academic.attendance.domain.AttendanceRecord;
import com.centerflow.academic.attendance.domain.AttendanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AttendanceRecordRepository
        extends JpaRepository<AttendanceRecord, UUID> {

    List<AttendanceRecord>
    findAllBySessionIdAndEnrollmentIdIn(
            UUID sessionId,
            Collection<UUID> enrollmentIds
    );

    @Query("""
            SELECT record
            FROM AttendanceRecord record
            WHERE record.sessionId = :sessionId
            AND (
                :status IS NULL
                OR record.status = :status
            )
            ORDER BY record.createdAt ASC
            """)
    Page<AttendanceRecord> searchBySession(
            @Param("sessionId") UUID sessionId,
            @Param("status") AttendanceStatus status,
            Pageable pageable
    );

    @Query("""
            SELECT record
            FROM AttendanceRecord record,
                 BatchSession session
            WHERE record.sessionId = session.id
            AND session.batchId = :batchId
            AND (
                :studentId IS NULL
                OR record.studentId = :studentId
            )
            AND (
                :enrollmentId IS NULL
                OR record.enrollmentId = :enrollmentId
            )
            AND (
                :status IS NULL
                OR record.status = :status
            )
            AND (
                :dateFrom IS NULL
                OR session.sessionDate >= :dateFrom
            )
            AND (
                :dateTo IS NULL
                OR session.sessionDate <= :dateTo
            )
            ORDER BY session.sessionDate DESC,
                     record.createdAt ASC
            """)
    Page<AttendanceRecord> searchByBatch(
            @Param("batchId") UUID batchId,
            @Param("studentId") UUID studentId,
            @Param("enrollmentId") UUID enrollmentId,
            @Param("status") AttendanceStatus status,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo,
            Pageable pageable
    );

    @Query("""
            SELECT record.status AS status,
                   COUNT(record) AS total
            FROM AttendanceRecord record
            WHERE record.sessionId = :sessionId
            GROUP BY record.status
            """)
    List<AttendanceStatusCount>
    countBySessionIdGroupedByStatus(
            @Param("sessionId") UUID sessionId
    );

    interface AttendanceStatusCount {

        AttendanceStatus getStatus();

        long getTotal();
    }
}