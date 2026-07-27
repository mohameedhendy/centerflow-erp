package com.centerflow.academic.seatreservation.repository;

import com.centerflow.academic.seatreservation.domain.SeatReservation;
import com.centerflow.academic.seatreservation.domain.SeatReservationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SeatReservationRepository
        extends JpaRepository<SeatReservation, UUID> {

    Optional<SeatReservation>
    findByBatchIdAndEnrollmentId(
            UUID batchId,
            UUID enrollmentId
    );

    long countByBatchIdAndStatus(
            UUID batchId,
            SeatReservationStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT reservation
            FROM SeatReservation reservation
            WHERE reservation.id = :reservationId
            """)
    Optional<SeatReservation> findByIdForUpdate(
            @Param("reservationId")
            UUID reservationId
    );
}