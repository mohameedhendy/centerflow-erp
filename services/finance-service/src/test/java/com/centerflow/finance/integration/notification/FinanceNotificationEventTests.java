package com.centerflow.finance.integration.notification;

import com.centerflow.finance.account.domain.EnrollmentFinancialAccount;
import com.centerflow.finance.payment.domain.Payment;
import com.centerflow.finance.payment.domain.PaymentMethod;
import com.centerflow.finance.refund.domain.Refund;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FinanceNotificationEventTests {

    @Test
    void paymentEventShouldContainStudentAndEnrollment() {
        EnrollmentFinancialAccount account =
                createAccount();

        Payment payment = Payment.create(
                "PAY-2026-000001",
                account.getId(),
                new BigDecimal("500.00"),
                "EGP",
                PaymentMethod.CASH,
                "PAYMENT-NOTIFICATION-1"
        );

        FinanceNotificationEvent firstEvent =
                FinanceNotificationEvent
                        .paymentRecorded(
                                payment,
                                account
                        );

        FinanceNotificationEvent secondEvent =
                FinanceNotificationEvent
                        .paymentRecorded(
                                payment,
                                account
                        );

        assertThat(firstEvent.type())
                .isEqualTo(
                        FinanceNotificationType
                                .PAYMENT_RECORDED
                );

        assertThat(firstEvent.recipientUserId())
                .isEqualTo(account.getStudentId());

        assertThat(firstEvent.referenceType())
                .isEqualTo("ENROLLMENT");

        assertThat(firstEvent.referenceId())
                .isEqualTo(account.getEnrollmentId());

        assertThat(firstEvent.sourceEventId())
                .isEqualTo(secondEvent.sourceEventId());

        assertThat(firstEvent.message())
                .contains(
                        payment.getPaymentNumber(),
                        "500.00",
                        "EGP"
                );
    }

    @Test
    void refundEventShouldUseDifferentSourceEvent() {
        EnrollmentFinancialAccount account =
                createAccount();

        Payment payment = Payment.create(
                "PAY-2026-000002",
                account.getId(),
                new BigDecimal("500.00"),
                "EGP",
                PaymentMethod.CARD,
                "PAYMENT-NOTIFICATION-2"
        );

        Refund refund = Refund.create(
                "REF-2026-000001",
                payment.getId(),
                new BigDecimal("200.00"),
                "EGP",
                "Partial cancellation",
                "REFUND-NOTIFICATION-1"
        );

        FinanceNotificationEvent paymentEvent =
                FinanceNotificationEvent
                        .paymentRecorded(
                                payment,
                                account
                        );

        FinanceNotificationEvent refundEvent =
                FinanceNotificationEvent
                        .paymentRefunded(
                                refund,
                                payment,
                                account
                        );

        assertThat(refundEvent.type())
                .isEqualTo(
                        FinanceNotificationType
                                .PAYMENT_REFUNDED
                );

        assertThat(refundEvent.sourceEventId())
                .isNotEqualTo(
                        paymentEvent.sourceEventId()
                );

        assertThat(refundEvent.message())
                .contains(
                        refund.getRefundNumber(),
                        payment.getPaymentNumber(),
                        "200.00",
                        "EGP"
                );
    }

    private EnrollmentFinancialAccount createAccount() {
        return EnrollmentFinancialAccount.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "NOTIFICATION-PLAN",
                new BigDecimal("1000.00"),
                "EGP",
                3,
                new BigDecimal("300.00")
        );
    }
}