package com.centerflow.academic.seatreservation.application;

import com.centerflow.academic.batch.domain.Batch;
import com.centerflow.academic.batch.domain.BatchStatus;
import com.centerflow.academic.batch.repository.BatchRepository;
import com.centerflow.academic.common.exception.BatchCapacityExceededException;
import com.centerflow.academic.common.exception.BatchNotFoundException;
import com.centerflow.academic.common.exception.BatchNotOpenForEnrollmentException;
import com.centerflow.academic.common.exception.DuplicateSeatReservationException;
import com.centerflow.academic.common.exception.SeatReservationNotFoundException;
import com.centerflow.academic.seatreservation.domain.SeatReservation;
import com.centerflow.academic.seatreservation.domain.SeatReservationStatus;
import com.centerflow.academic.seatreservation.repository.SeatReservationRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class SeatReservationService {

    private final SeatReservationRepository
            seatReservationRepository;

    private final BatchRepository batchRepository;
    private final Clock clock;

    public SeatReservationService(
            SeatReservationRepository
                    seatReservationRepository,
            BatchRepository batchRepository,
            Clock clock
    ) {
        this.seatReservationRepository =
                seatReservationRepository;

        this.batchRepository = batchRepository;
        this.clock = clock;
    }

    @Transactional
    public SeatReservationResult reserve(
            UUID batchId,
            UUID enrollmentId
    ) {
        Batch batch = findBatchForUpdate(batchId);

        validateBatchOpenForEnrollment(batch);

        SeatReservation existingReservation =
                seatReservationRepository
                        .findByBatchIdAndEnrollmentId(
                                batchId,
                                enrollmentId
                        )
                        .orElse(null);

        if (existingReservation != null
                && existingReservation.isReserved()) {
            return SeatReservationResult.from(
                    existingReservation
            );
        }

        long reservedSeats = countReservedSeats(batchId);

        if (reservedSeats >= batch.getCapacity()) {
            throw new BatchCapacityExceededException(
                    batchId
            );
        }

        Instant now = Instant.now(clock);

        if (existingReservation != null) {
            existingReservation.reserve(now);

            seatReservationRepository.flush();

            return SeatReservationResult.from(
                    existingReservation
            );
        }

        SeatReservation reservation =
                SeatReservation.create(
                        batchId,
                        enrollmentId,
                        now
                );

        try {
            SeatReservation savedReservation =
                    seatReservationRepository
                            .saveAndFlush(reservation);

            return SeatReservationResult.from(
                    savedReservation
            );

        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateSeatReservationException(
                    batchId,
                    enrollmentId,
                    exception
            );
        }
    }

    @Transactional
    public SeatReservationResult release(
            UUID reservationId
    ) {
        SeatReservation existingReservation =
                seatReservationRepository
                        .findById(reservationId)
                        .orElseThrow(
                                () ->
                                        new SeatReservationNotFoundException(
                                                reservationId
                                        )
                        );

        findBatchForUpdate(
                existingReservation.getBatchId()
        );

        SeatReservation reservation =
                seatReservationRepository
                        .findByIdForUpdate(reservationId)
                        .orElseThrow(
                                () ->
                                        new SeatReservationNotFoundException(
                                                reservationId
                                        )
                        );

        reservation.release(
                Instant.now(clock)
        );

        return SeatReservationResult.from(
                reservation
        );
    }

    @Transactional(readOnly = true)
    public SeatReservationResult getById(
            UUID reservationId
    ) {
        SeatReservation reservation =
                seatReservationRepository
                        .findById(reservationId)
                        .orElseThrow(
                                () ->
                                        new SeatReservationNotFoundException(
                                                reservationId
                                        )
                        );

        return SeatReservationResult.from(
                reservation
        );
    }

    @Transactional(readOnly = true)
    public SeatAvailabilityResult getAvailability(
            UUID batchId
    ) {
        Batch batch = batchRepository
                .findById(batchId)
                .orElseThrow(
                        () -> new BatchNotFoundException(
                                batchId
                        )
                );

        long reservedSeats =
                countReservedSeats(batchId);

        long availableSeats =
                Math.max(
                        0,
                        (long) batch.getCapacity()
                                - reservedSeats
                );

        return new SeatAvailabilityResult(
                batch.getId(),
                batch.getStatus(),
                batch.getCapacity(),
                reservedSeats,
                availableSeats,
                availableSeats == 0
        );
    }

    private Batch findBatchForUpdate(
            UUID batchId
    ) {
        return batchRepository
                .findByIdForUpdate(batchId)
                .orElseThrow(
                        () -> new BatchNotFoundException(
                                batchId
                        )
                );
    }

    private void validateBatchOpenForEnrollment(
            Batch batch
    ) {
        if (batch.getStatus()
                != BatchStatus.OPEN_FOR_ENROLLMENT) {
            throw new BatchNotOpenForEnrollmentException(
                    batch.getStatus()
            );
        }
    }

    private long countReservedSeats(UUID batchId) {
        return seatReservationRepository
                .countByBatchIdAndStatus(
                        batchId,
                        SeatReservationStatus.RESERVED
                );
    }
}