package com.centerflow.finance.account.application;

import com.centerflow.finance.account.domain.EnrollmentFinancialAccount;
import com.centerflow.finance.account.domain.Installment;
import com.centerflow.finance.account.domain.InstallmentStatus;
import com.centerflow.finance.account.repository.EnrollmentFinancialAccountRepository;
import com.centerflow.finance.account.repository.InstallmentRepository;
import com.centerflow.finance.integration.notification.FinanceNotificationEvent;
import com.centerflow.finance.integration.notification.FinanceNotificationType;
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
class OverdueInstallmentNotificationIntegrationTests {

    @Autowired
    private InstallmentCollectionService
            installmentCollectionService;

    @Autowired
    private PricingPlanRepository
            pricingPlanRepository;

    @Autowired
    private EnrollmentFinancialAccountRepository
            financialAccountRepository;

    @Autowired
    private InstallmentRepository
            installmentRepository;

    @Autowired
    private ApplicationEvents
            applicationEvents;

    @Test
    void newlyOverdueInstallmentShouldPublishOneNotificationEvent() {
        EnrollmentFinancialAccount account =
                createAccount();

        Installment pastInstallment =
                Installment.create(
                        account.getId(),
                        1,
                        LocalDate.of(
                                2026,
                                7,
                                30
                        ),
                        new BigDecimal("300.00")
                );

        Installment dueTodayInstallment =
                Installment.create(
                        account.getId(),
                        2,
                        LocalDate.of(
                                2026,
                                7,
                                31
                        ),
                        new BigDecimal("300.00")
                );

        installmentRepository.saveAllAndFlush(
                List.of(
                        pastInstallment,
                        dueTodayInstallment
                )
        );

        var firstResult =
                installmentCollectionService
                        .markOverdueInstallments(
                                LocalDate.of(
                                        2026,
                                        7,
                                        31
                                )
                        );

        assertThat(
                firstResult.markedOverdueCount()
        ).isEqualTo(1);

        assertThat(
                installmentRepository
                        .findById(
                                pastInstallment.getId()
                        )
                        .orElseThrow()
                        .getStatus()
        ).isEqualTo(
                InstallmentStatus.OVERDUE
        );

        assertThat(
                installmentRepository
                        .findById(
                                dueTodayInstallment.getId()
                        )
                        .orElseThrow()
                        .getStatus()
        ).isEqualTo(
                InstallmentStatus.PENDING
        );

        List<FinanceNotificationEvent> events =
                applicationEvents
                        .stream(
                                FinanceNotificationEvent.class
                        )
                        .toList();

        assertThat(events).hasSize(1);

        FinanceNotificationEvent event =
                events.getFirst();

        assertThat(event.recipientUserId())
                .isEqualTo(account.getStudentId());

        assertThat(event.type())
                .isEqualTo(
                        FinanceNotificationType
                                .INSTALLMENT_OVERDUE
                );

        assertThat(event.referenceType())
                .isEqualTo("INSTALLMENT");

        assertThat(event.referenceId())
                .isEqualTo(
                        pastInstallment.getId()
                );

        assertThat(event.message())
                .contains("300.00 EGP")
                .contains("2026-07-30");

        var repeatedResult =
                installmentCollectionService
                        .markOverdueInstallments(
                                LocalDate.of(
                                        2026,
                                        7,
                                        31
                                )
                        );

        assertThat(
                repeatedResult.markedOverdueCount()
        ).isZero();

        assertThat(
                applicationEvents
                        .stream(
                                FinanceNotificationEvent.class
                        )
        ).hasSize(1);
    }

    private EnrollmentFinancialAccount createAccount() {
        String planCode =
                "OVERDUE-NOTIFY-"
                        + UUID.randomUUID()
                        .toString()
                        .substring(0, 8);

        PricingPlan pricingPlan =
                PricingPlan.create(
                        planCode,
                        "Overdue Notification Plan",
                        "Overdue notification test plan",
                        new BigDecimal("600.00"),
                        "EGP",
                        2,
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

        return financialAccountRepository
                .saveAndFlush(account);
    }
}