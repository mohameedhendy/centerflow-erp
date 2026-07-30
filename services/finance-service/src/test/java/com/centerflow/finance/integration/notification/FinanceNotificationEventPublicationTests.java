package com.centerflow.finance.integration.notification;

import com.centerflow.finance.account.domain.EnrollmentFinancialAccount;
import com.centerflow.finance.account.domain.Installment;
import com.centerflow.finance.account.domain.InstallmentScheduleGenerator;
import com.centerflow.finance.account.repository.EnrollmentFinancialAccountRepository;
import com.centerflow.finance.account.repository.InstallmentRepository;
import com.centerflow.finance.payment.api.dto.PaymentResponse;
import com.centerflow.finance.payment.api.dto.RecordPaymentRequest;
import com.centerflow.finance.payment.application.PaymentService;
import com.centerflow.finance.payment.domain.PaymentMethod;
import com.centerflow.finance.pricing.domain.PricingPlan;
import com.centerflow.finance.pricing.repository.PricingPlanRepository;
import com.centerflow.finance.refund.api.dto.RecordRefundRequest;
import com.centerflow.finance.refund.application.RefundService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@RecordApplicationEvents
class FinanceNotificationEventPublicationTests {

    @Autowired
    private ApplicationEvents applicationEvents;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private RefundService refundService;

    @Autowired
    private PricingPlanRepository pricingPlanRepository;

    @Autowired
    private EnrollmentFinancialAccountRepository
            financialAccountRepository;

    @Autowired
    private InstallmentRepository installmentRepository;

    @Test
    void paymentShouldPublishNotificationEvent() {
        EnrollmentFinancialAccount account =
                createAccount(
                        "NOTIFICATION-PAYMENT-PLAN"
                );

        PaymentResponse payment =
                paymentService.recordPayment(
                        account.getEnrollmentId(),
                        new RecordPaymentRequest(
                                new BigDecimal("500.00"),
                                PaymentMethod.CASH,
                                "PAY-NOTIFY-"
                                        + UUID.randomUUID()
                        )
                );

        List<FinanceNotificationEvent> events =
                applicationEvents
                        .stream(
                                FinanceNotificationEvent.class
                        )
                        .filter(event ->
                                event.type()
                                        == FinanceNotificationType
                                        .PAYMENT_RECORDED
                        )
                        .toList();

        assertThat(events).hasSize(1);

        FinanceNotificationEvent event =
                events.getFirst();

        assertThat(event.recipientUserId())
                .isEqualTo(account.getStudentId());

        assertThat(event.referenceId())
                .isEqualTo(account.getEnrollmentId());

        assertThat(event.message())
                .contains(payment.paymentNumber());
    }

    @Test
    void refundShouldPublishNotificationEvent() {
        EnrollmentFinancialAccount account =
                createAccount(
                        "NOTIFICATION-REFUND-PLAN"
                );

        PaymentResponse payment =
                paymentService.recordPayment(
                        account.getEnrollmentId(),
                        new RecordPaymentRequest(
                                new BigDecimal("500.00"),
                                PaymentMethod.CARD,
                                "PAY-NOTIFY-"
                                        + UUID.randomUUID()
                        )
                );

        refundService.recordRefund(
                payment.id(),
                new RecordRefundRequest(
                        new BigDecimal("200.00"),
                        "Partial cancellation",
                        "REF-NOTIFY-"
                                + UUID.randomUUID()
                )
        );

        List<FinanceNotificationEvent> refundEvents =
                applicationEvents
                        .stream(
                                FinanceNotificationEvent.class
                        )
                        .filter(event ->
                                event.type()
                                        == FinanceNotificationType
                                        .PAYMENT_REFUNDED
                        )
                        .toList();

        assertThat(refundEvents).hasSize(1);

        FinanceNotificationEvent event =
                refundEvents.getFirst();

        assertThat(event.recipientUserId())
                .isEqualTo(account.getStudentId());

        assertThat(event.referenceId())
                .isEqualTo(account.getEnrollmentId());

        assertThat(event.message())
                .contains(payment.paymentNumber());
    }

    private EnrollmentFinancialAccount createAccount(
            String pricingPlanCode
    ) {
        PricingPlan pricingPlan = PricingPlan.create(
                pricingPlanCode,
                "Notification Test Plan",
                "Plan used for notification integration tests",
                new BigDecimal("1000.00"),
                "EGP",
                3,
                new BigDecimal("300.00")
        );

        pricingPlanRepository.saveAndFlush(
                pricingPlan
        );

        EnrollmentFinancialAccount account =
                EnrollmentFinancialAccount.create(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        pricingPlan.getId(),
                        pricingPlan.getCode(),
                        pricingPlan.getTotalAmount(),
                        pricingPlan.getCurrency(),
                        pricingPlan.getInstallmentCount(),
                        pricingPlan
                                .getInitialPaymentAmount()
                );

        financialAccountRepository.saveAndFlush(
                account
        );

        List<Installment> installments =
                InstallmentScheduleGenerator
                        .generate(
                                account.getTotalAmount(),
                                account.getInstallmentCount(),
                                LocalDate.of(2026, 8, 1)
                        )
                        .stream()
                        .map(item ->
                                Installment.create(
                                        account.getId(),
                                        item.installmentNumber(),
                                        item.dueDate(),
                                        item.amount()
                                )
                        )
                        .toList();

        installmentRepository.saveAllAndFlush(
                installments
        );

        return account;
    }
}