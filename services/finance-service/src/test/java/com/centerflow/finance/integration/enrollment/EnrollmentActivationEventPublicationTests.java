package com.centerflow.finance.integration.enrollment;

import com.centerflow.finance.account.domain.EnrollmentFinancialAccount;
import com.centerflow.finance.account.domain.Installment;
import com.centerflow.finance.account.domain.InstallmentScheduleGenerator;
import com.centerflow.finance.account.repository.EnrollmentFinancialAccountRepository;
import com.centerflow.finance.account.repository.InstallmentRepository;
import com.centerflow.finance.payment.api.dto.RecordPaymentRequest;
import com.centerflow.finance.payment.application.PaymentService;
import com.centerflow.finance.payment.domain.PaymentMethod;
import com.centerflow.finance.pricing.domain.PricingPlan;
import com.centerflow.finance.pricing.repository.PricingPlanRepository;
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
class EnrollmentActivationEventPublicationTests {

    @Autowired
    private ApplicationEvents applicationEvents;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PricingPlanRepository pricingPlanRepository;

    @Autowired
    private EnrollmentFinancialAccountRepository
            financialAccountRepository;

    @Autowired
    private InstallmentRepository installmentRepository;

    @Autowired
    private EnrollmentActivationTaskRepository
            activationTaskRepository;

    @Test
    void activationShouldBeRequestedOnlyWhenThresholdCrosses() {
        EnrollmentFinancialAccount account =
                createAccount();

        paymentService.recordPayment(
                account.getEnrollmentId(),
                new RecordPaymentRequest(
                        new BigDecimal("100.00"),
                        PaymentMethod.CASH,
                        "ACTIVATION-"
                                + UUID.randomUUID()
                )
        );

        assertThat(
                applicationEvents
                        .stream(
                                EnrollmentActivationRequested.class
                        )
        ).isEmpty();

        assertThat(activationTaskRepository.count())
                .isZero();

        paymentService.recordPayment(
                account.getEnrollmentId(),
                new RecordPaymentRequest(
                        new BigDecimal("200.00"),
                        PaymentMethod.CARD,
                        "ACTIVATION-"
                                + UUID.randomUUID()
                )
        );

        List<EnrollmentActivationRequested> events =
                applicationEvents
                        .stream(
                                EnrollmentActivationRequested.class
                        )
                        .toList();

        assertThat(events).hasSize(1);

        assertThat(activationTaskRepository.count())
                .isEqualTo(1);

        EnrollmentActivationTask task =
                activationTaskRepository
                        .findByEnrollmentId(
                                account.getEnrollmentId()
                        )
                        .orElseThrow();

        assertThat(events.getFirst().taskId())
                .isEqualTo(task.getId());

        paymentService.recordPayment(
                account.getEnrollmentId(),
                new RecordPaymentRequest(
                        new BigDecimal("50.00"),
                        PaymentMethod.CASH,
                        "ACTIVATION-"
                                + UUID.randomUUID()
                )
        );

        assertThat(
                applicationEvents
                        .stream(
                                EnrollmentActivationRequested.class
                        )
                        .toList()
        ).hasSize(1);

        assertThat(activationTaskRepository.count())
                .isEqualTo(1);
    }

    private EnrollmentFinancialAccount createAccount() {
        PricingPlan pricingPlan = PricingPlan.create(
                "ACTIVATION-PLAN",
                "Activation Plan",
                "Plan used for activation integration tests",
                new BigDecimal("1000.00"),
                "EGP",
                3,
                new BigDecimal("300.00")
        );

        pricingPlanRepository.saveAndFlush(pricingPlan);

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

        financialAccountRepository.saveAndFlush(account);

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