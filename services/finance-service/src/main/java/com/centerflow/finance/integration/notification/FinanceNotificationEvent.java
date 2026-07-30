package com.centerflow.finance.integration.notification;

import com.centerflow.finance.account.domain.EnrollmentFinancialAccount;
import com.centerflow.finance.payment.domain.Payment;
import com.centerflow.finance.refund.domain.Refund;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public record FinanceNotificationEvent(

        UUID sourceEventId,
        UUID recipientUserId,
        FinanceNotificationType type,
        String title,
        String message,
        String referenceType,
        UUID referenceId

) {

    public FinanceNotificationEvent {
        Objects.requireNonNull(
                sourceEventId,
                "Source event ID is required"
        );

        Objects.requireNonNull(
                recipientUserId,
                "Recipient user ID is required"
        );

        Objects.requireNonNull(
                type,
                "Notification type is required"
        );

        title = normalizeRequiredText(
                title,
                "Notification title"
        );

        message = normalizeRequiredText(
                message,
                "Notification message"
        );

        referenceType = normalizeRequiredText(
                referenceType,
                "Reference type"
        ).toUpperCase(Locale.ROOT);

        Objects.requireNonNull(
                referenceId,
                "Reference ID is required"
        );
    }

    public static FinanceNotificationEvent
    paymentRecorded(
            Payment payment,
            EnrollmentFinancialAccount account
    ) {
        Objects.requireNonNull(payment);
        Objects.requireNonNull(account);

        return new FinanceNotificationEvent(
                createSourceEventId(
                        FinanceNotificationType
                                .PAYMENT_RECORDED,
                        payment.getId()
                ),
                account.getStudentId(),
                FinanceNotificationType
                        .PAYMENT_RECORDED,
                "Payment received",
                "Payment "
                        + payment.getPaymentNumber()
                        + " of "
                        + payment.getAmount().toPlainString()
                        + " "
                        + payment.getCurrency()
                        + " was recorded successfully.",
                "ENROLLMENT",
                account.getEnrollmentId()
        );
    }

    public static FinanceNotificationEvent
    paymentRefunded(
            Refund refund,
            Payment payment,
            EnrollmentFinancialAccount account
    ) {
        Objects.requireNonNull(refund);
        Objects.requireNonNull(payment);
        Objects.requireNonNull(account);

        return new FinanceNotificationEvent(
                createSourceEventId(
                        FinanceNotificationType
                                .PAYMENT_REFUNDED,
                        refund.getId()
                ),
                account.getStudentId(),
                FinanceNotificationType
                        .PAYMENT_REFUNDED,
                "Payment refunded",
                "Refund "
                        + refund.getRefundNumber()
                        + " of "
                        + refund.getAmount().toPlainString()
                        + " "
                        + refund.getCurrency()
                        + " was recorded for payment "
                        + payment.getPaymentNumber()
                        + ".",
                "ENROLLMENT",
                account.getEnrollmentId()
        );
    }

    private static UUID createSourceEventId(
            FinanceNotificationType type,
            UUID sourceId
    ) {
        String sourceValue =
                type.name()
                        + ":"
                        + sourceId;

        return UUID.nameUUIDFromBytes(
                sourceValue.getBytes(
                        StandardCharsets.UTF_8
                )
        );
    }

    private static String normalizeRequiredText(
            String value,
            String fieldName
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " is required"
            );
        }

        return value.trim();
    }
}