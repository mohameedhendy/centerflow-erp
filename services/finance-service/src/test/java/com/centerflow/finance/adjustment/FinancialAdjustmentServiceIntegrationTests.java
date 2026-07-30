package com.centerflow.finance.adjustment;

import com.centerflow.finance.account.domain.EnrollmentFinancialAccount;
import com.centerflow.finance.account.domain.Installment;
import com.centerflow.finance.account.domain.InstallmentScheduleGenerator;
import com.centerflow.finance.account.repository.EnrollmentFinancialAccountRepository;
import com.centerflow.finance.account.repository.InstallmentRepository;
import com.centerflow.finance.adjustment.api.dto.RecordFinancialAdjustmentRequest;
import com.centerflow.finance.adjustment.application.FinancialAdjustmentService;
import com.centerflow.finance.adjustment.domain.FinancialAdjustmentType;
import com.centerflow.finance.adjustment.exception.FinancialAdjustmentConflictException;
import com.centerflow.finance.adjustment.repository.FinancialAdjustmentRepository;
import com.centerflow.finance.pricing.domain.PricingPlan;
import com.centerflow.finance.pricing.repository.PricingPlanRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FinancialAdjustmentServiceIntegrationTests {

    @Autowired
    private FinancialAdjustmentService adjustmentService;

    @Autowired
    private FinancialAdjustmentRepository adjustmentRepository;

    @Autowired
    private EnrollmentFinancialAccountRepository
            accountRepository;

    @Autowired
    private InstallmentRepository installmentRepository;

    @Autowired
    private PricingPlanRepository pricingPlanRepository;

    @Test
    void discountAndChargeShouldUpdateAccountAndInstallments() {
        EnrollmentFinancialAccount account =
                createFinancialAccount();

        adjustmentService.recordAdjustment(
                account.getEnrollmentId(),
                new RecordFinancialAdjustmentRequest(
                        FinancialAdjustmentType.DISCOUNT,
                        new BigDecimal("150.00"),
                        "Student scholarship",
                        "DISCOUNT-" + UUID.randomUUID()
                )
        );

        adjustmentService.recordAdjustment(
                account.getEnrollmentId(),
                new RecordFinancialAdjustmentRequest(
                        FinancialAdjustmentType.CHARGE,
                        new BigDecimal("50.00"),
                        "Additional learning materials",
                        "CHARGE-" + UUID.randomUUID()
                )
        );

        EnrollmentFinancialAccount updatedAccount =
                accountRepository
                        .findByEnrollmentId(
                                account.getEnrollmentId()
                        )
                        .orElseThrow();

        assertThat(updatedAccount.getTotalAmount())
                .isEqualByComparingTo("900.00");

        assertThat(updatedAccount.getRemainingAmount())
                .isEqualByComparingTo("900.00");

        BigDecimal installmentTotal =
                installmentRepository
                        .findAll()
                        .stream()
                        .filter(installment ->
                                installment
                                        .getFinancialAccountId()
                                        .equals(account.getId())
                        )
                        .map(Installment::getAmount)
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        assertThat(installmentTotal)
                .isEqualByComparingTo("900.00");

        long adjustmentCount =
                adjustmentRepository
                        .findAllByFinancialAccountId(
                                account.getId(),
                                PageRequest.of(0, 10)
                        )
                        .getTotalElements();

        assertThat(adjustmentCount).isEqualTo(2);
    }

    @Test
    void duplicateExternalReferenceShouldBeRejected() {
        EnrollmentFinancialAccount account =
                createFinancialAccount();

        String externalReference =
                "ADJUSTMENT-" + UUID.randomUUID();

        RecordFinancialAdjustmentRequest request =
                new RecordFinancialAdjustmentRequest(
                        FinancialAdjustmentType.DISCOUNT,
                        new BigDecimal("100.00"),
                        "Promotional discount",
                        externalReference
                );

        adjustmentService.recordAdjustment(
                account.getEnrollmentId(),
                request
        );

        assertThatThrownBy(
                () -> adjustmentService.recordAdjustment(
                        account.getEnrollmentId(),
                        request
                )
        )
                .isInstanceOf(
                        FinancialAdjustmentConflictException.class
                )
                .hasMessageContaining(
                        externalReference
                );
    }

    @Test
    void discountGreaterThanRemainingBalanceShouldBeRejected() {
        EnrollmentFinancialAccount account =
                createFinancialAccount();

        assertThatThrownBy(
                () -> adjustmentService.recordAdjustment(
                        account.getEnrollmentId(),
                        new RecordFinancialAdjustmentRequest(
                                FinancialAdjustmentType.DISCOUNT,
                                new BigDecimal("1001.00"),
                                "Invalid excessive discount",
                                "INVALID-" + UUID.randomUUID()
                        )
                )
        )
                .isInstanceOf(
                        FinancialAdjustmentConflictException.class
                )
                .hasMessageContaining(
                        "remaining balance"
                );

        assertThat(adjustmentRepository.count())
                .isZero();
    }

    private EnrollmentFinancialAccount
    createFinancialAccount() {
        String planCode =
                "ADJ-"
                        + UUID.randomUUID()
                        .toString()
                        .substring(0, 12);

        PricingPlan pricingPlan = PricingPlan.create(
                planCode,
                "Financial Adjustment Plan",
                "Plan used for adjustment integration tests",
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
                        pricingPlan.getInitialPaymentAmount()
                );

        accountRepository.saveAndFlush(account);

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