package com.centerflow.finance.account.application;

import com.centerflow.finance.account.api.dto.CreateEnrollmentFinancialAccountRequest;
import com.centerflow.finance.account.api.dto.EnrollmentFinancialAccountResponse;
import com.centerflow.finance.account.domain.EnrollmentFinancialAccount;
import com.centerflow.finance.account.domain.Installment;
import com.centerflow.finance.account.domain.InstallmentScheduleGenerator;
import com.centerflow.finance.account.exception.EnrollmentFinancialAccountConflictException;
import com.centerflow.finance.account.exception.EnrollmentFinancialAccountNotFoundException;
import com.centerflow.finance.account.repository.EnrollmentFinancialAccountRepository;
import com.centerflow.finance.account.repository.InstallmentRepository;
import com.centerflow.finance.pricing.domain.PricingPlan;
import com.centerflow.finance.pricing.domain.PricingPlanStatus;
import com.centerflow.finance.pricing.exception.PricingPlanConflictException;
import com.centerflow.finance.pricing.exception.PricingPlanNotFoundException;
import com.centerflow.finance.pricing.repository.PricingPlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class EnrollmentFinancialAccountService {

    private final EnrollmentFinancialAccountRepository
            financialAccountRepository;

    private final InstallmentRepository installmentRepository;
    private final PricingPlanRepository pricingPlanRepository;

    public EnrollmentFinancialAccountService(
            EnrollmentFinancialAccountRepository
                    financialAccountRepository,
            InstallmentRepository installmentRepository,
            PricingPlanRepository pricingPlanRepository
    ) {
        this.financialAccountRepository =
                financialAccountRepository;
        this.installmentRepository = installmentRepository;
        this.pricingPlanRepository = pricingPlanRepository;
    }

    @Transactional
    public EnrollmentFinancialAccountResponse
    createFinancialAccount(
            CreateEnrollmentFinancialAccountRequest request
    ) {
        if (
                financialAccountRepository
                        .existsByEnrollmentId(
                                request.enrollmentId()
                        )
        ) {
            throw new EnrollmentFinancialAccountConflictException(
                    "Financial account already exists for "
                            + "enrollment: "
                            + request.enrollmentId()
            );
        }

        PricingPlan pricingPlan = pricingPlanRepository
                .findById(request.pricingPlanId())
                .orElseThrow(
                        () -> new PricingPlanNotFoundException(
                                request.pricingPlanId()
                        )
                );

        if (
                pricingPlan.getStatus()
                        != PricingPlanStatus.ACTIVE
        ) {
            throw new PricingPlanConflictException(
                    "Pricing plan is not active: "
                            + pricingPlan.getId()
            );
        }

        EnrollmentFinancialAccount account =
                EnrollmentFinancialAccount.create(
                        request.enrollmentId(),
                        request.studentId(),
                        pricingPlan.getId(),
                        pricingPlan.getCode(),
                        pricingPlan.getTotalAmount(),
                        pricingPlan.getCurrency(),
                        pricingPlan.getInstallmentCount(),
                        pricingPlan.getInitialPaymentAmount()
                );

        EnrollmentFinancialAccount savedAccount =
                financialAccountRepository.save(account);

        List<Installment> installments =
                InstallmentScheduleGenerator
                        .generate(
                                savedAccount.getTotalAmount(),
                                savedAccount
                                        .getInstallmentCount(),
                                request
                                        .firstInstallmentDueDate()
                        )
                        .stream()
                        .map(item ->
                                Installment.create(
                                        savedAccount.getId(),
                                        item.installmentNumber(),
                                        item.dueDate(),
                                        item.amount()
                                )
                        )
                        .toList();

        List<Installment> savedInstallments =
                installmentRepository.saveAll(installments)
                        .stream()
                        .sorted(
                                Comparator.comparingInt(
                                        Installment
                                                ::getInstallmentNumber
                                )
                        )
                        .toList();

        return EnrollmentFinancialAccountResponse.from(
                savedAccount,
                savedInstallments
        );
    }

    @Transactional(readOnly = true)
    public EnrollmentFinancialAccountResponse
    getFinancialAccount(
            UUID enrollmentId
    ) {
        EnrollmentFinancialAccount account =
                financialAccountRepository
                        .findByEnrollmentId(enrollmentId)
                        .orElseThrow(
                                () ->
                                        new EnrollmentFinancialAccountNotFoundException(
                                                enrollmentId
                                        )
                        );

        List<Installment> installments =
                installmentRepository
                        .findAllByFinancialAccountIdOrderByInstallmentNumberAsc(
                                account.getId()
                        );

        return EnrollmentFinancialAccountResponse.from(
                account,
                installments
        );
    }
}