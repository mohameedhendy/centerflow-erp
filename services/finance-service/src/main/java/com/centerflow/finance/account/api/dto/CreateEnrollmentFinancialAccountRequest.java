package com.centerflow.finance.account.api.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record CreateEnrollmentFinancialAccountRequest(

        @NotNull(message = "Enrollment ID is required")
        UUID enrollmentId,

        @NotNull(message = "Student ID is required")
        UUID studentId,

        @NotNull(message = "Pricing plan ID is required")
        UUID pricingPlanId,

        @NotNull(
                message = "First installment due date is required"
        )
        LocalDate firstInstallmentDueDate

) {
}