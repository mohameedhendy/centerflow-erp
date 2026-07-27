package com.centerflow.academic.seatreservation.api;

import com.centerflow.academic.seatreservation.application.SeatAvailabilityResult;
import com.centerflow.academic.seatreservation.application.SeatReservationResult;
import com.centerflow.academic.seatreservation.application.SeatReservationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/academic/internal")
public class SeatReservationController {

    private final SeatReservationService
            seatReservationService;

    public SeatReservationController(
            SeatReservationService
                    seatReservationService
    ) {
        this.seatReservationService =
                seatReservationService;
    }

    @PostMapping(
            "/batches/{batchId}/seat-reservations"
    )
    public SeatReservationResponse reserve(
            @PathVariable UUID batchId,

            @Valid @RequestBody
            ReserveSeatRequest request
    ) {
        SeatReservationResult result =
                seatReservationService.reserve(
                        batchId,
                        request.enrollmentId()
                );

        return SeatReservationResponse.from(result);
    }

    @PostMapping(
            "/seat-reservations/{reservationId}/release"
    )
    public SeatReservationResponse release(
            @PathVariable UUID reservationId
    ) {
        SeatReservationResult result =
                seatReservationService.release(
                        reservationId
                );

        return SeatReservationResponse.from(result);
    }

    @GetMapping(
            "/seat-reservations/{reservationId}"
    )
    public SeatReservationResponse getById(
            @PathVariable UUID reservationId
    ) {
        return SeatReservationResponse.from(
                seatReservationService.getById(
                        reservationId
                )
        );
    }

    @GetMapping(
            "/batches/{batchId}/seat-availability"
    )
    public SeatAvailabilityResponse getAvailability(
            @PathVariable UUID batchId
    ) {
        SeatAvailabilityResult result =
                seatReservationService
                        .getAvailability(batchId);

        return SeatAvailabilityResponse.from(result);
    }
}