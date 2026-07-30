package com.centerflow.finance.earning.api.dto;

import com.centerflow.finance.earning.domain.InstructorEarning;
import com.centerflow.finance.earning.domain.InstructorEarningStatus;
import com.centerflow.finance.payment.domain.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record InstructorEarningResponse(

        UUID id,
        String earningNumber,
        UUID instructorId,
        UUID sessionId,
        UUID batchId,
        BigDecimal amount,
        String currency,
        LocalDate sessionDate,
        String description,
        InstructorEarningStatus status,
        PaymentMethod paymentMethod,
        String paymentReference,
        String cancellationReason,
        Instant accruedAt,
        Instant paidAt,
        Instant cancelledAt,
        Instant createdAt,
        Instant updatedAt

) {

    public static InstructorEarningResponse from(
            InstructorEarning earning
    ) {
        return new InstructorEarningResponse(
                earning.getId(),
                earning.getEarningNumber(),
                earning.getInstructorId(),
                earning.getSessionId(),
                earning.getBatchId(),
                earning.getAmount(),
                earning.getCurrency(),
                earning.getSessionDate(),
                earning.getDescription(),
                earning.getStatus(),
                earning.getPaymentMethod(),
                earning.getPaymentReference(),
                earning.getCancellationReason(),
                earning.getAccruedAt(),
                earning.getPaidAt(),
                earning.getCancelledAt(),
                earning.getCreatedAt(),
                earning.getUpdatedAt()
        );
    }
}