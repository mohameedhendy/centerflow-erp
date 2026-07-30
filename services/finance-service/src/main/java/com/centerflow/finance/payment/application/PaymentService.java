package com.centerflow.finance.payment.application;

import com.centerflow.finance.account.domain.EnrollmentFinancialAccount;
import com.centerflow.finance.account.domain.Installment;
import com.centerflow.finance.account.domain.InstallmentStatus;
import com.centerflow.finance.account.exception.EnrollmentFinancialAccountNotFoundException;
import com.centerflow.finance.account.repository.EnrollmentFinancialAccountRepository;
import com.centerflow.finance.account.repository.InstallmentRepository;
import com.centerflow.finance.payment.api.dto.PaymentResponse;
import com.centerflow.finance.payment.api.dto.RecordPaymentRequest;
import com.centerflow.finance.payment.domain.Payment;
import com.centerflow.finance.payment.domain.PaymentAllocation;
import com.centerflow.finance.payment.exception.PaymentConflictException;
import com.centerflow.finance.payment.exception.PaymentNotFoundException;
import com.centerflow.finance.payment.number.PaymentNumberGenerator;
import com.centerflow.finance.payment.repository.PaymentAllocationRepository;
import com.centerflow.finance.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {

    private final EnrollmentFinancialAccountRepository
            financialAccountRepository;

    private final InstallmentRepository installmentRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentAllocationRepository
            paymentAllocationRepository;

    private final PaymentNumberGenerator
            paymentNumberGenerator;

    public PaymentService(
            EnrollmentFinancialAccountRepository
                    financialAccountRepository,
            InstallmentRepository installmentRepository,
            PaymentRepository paymentRepository,
            PaymentAllocationRepository
                    paymentAllocationRepository,
            PaymentNumberGenerator paymentNumberGenerator
    ) {
        this.financialAccountRepository =
                financialAccountRepository;
        this.installmentRepository = installmentRepository;
        this.paymentRepository = paymentRepository;
        this.paymentAllocationRepository =
                paymentAllocationRepository;
        this.paymentNumberGenerator =
                paymentNumberGenerator;
    }

    @Transactional
    public PaymentResponse recordPayment(
            UUID enrollmentId,
            RecordPaymentRequest request
    ) {
        EnrollmentFinancialAccount account =
                financialAccountRepository
                        .findByEnrollmentIdForUpdate(
                                enrollmentId
                        )
                        .orElseThrow(
                                () ->
                                        new EnrollmentFinancialAccountNotFoundException(
                                                enrollmentId
                                        )
                        );

        BigDecimal paymentAmount =
                normalizePaymentAmount(request.amount());

        String externalReference =
                normalizeExternalReference(
                        request.externalReference()
                );

        if (
                externalReference != null
                        && paymentRepository
                        .existsByExternalReference(
                                externalReference
                        )
        ) {
            throw new PaymentConflictException(
                    "Payment external reference "
                            + "already exists: "
                            + externalReference
            );
        }

        if (
                paymentAmount.compareTo(
                        account.getRemainingAmount()
                ) > 0
        ) {
            throw new PaymentConflictException(
                    "Payment amount exceeds remaining balance"
            );
        }

        List<Installment> installments =
                installmentRepository
                        .findAllByFinancialAccountIdForUpdate(
                                account.getId()
                        );

        Payment payment = Payment.create(
                paymentNumberGenerator.nextNumber(),
                account.getId(),
                paymentAmount,
                account.getCurrency(),
                request.method(),
                externalReference
        );

        Payment savedPayment =
                paymentRepository.saveAndFlush(payment);

        BigDecimal remainingToAllocate =
                paymentAmount;

        List<PaymentAllocation> allocations =
                new ArrayList<>();

        int allocationOrder = 1;

        for (Installment installment : installments) {
            if (remainingToAllocate.signum() == 0) {
                break;
            }

            if (
                    installment.getStatus()
                            == InstallmentStatus.PAID
                            || installment.getStatus()
                            == InstallmentStatus.CANCELLED
            ) {
                continue;
            }

            BigDecimal allocationAmount =
                    remainingToAllocate.min(
                            installment
                                    .getRemainingAmount()
                    );

            installment.allocatePayment(
                    allocationAmount
            );

            allocations.add(
                    PaymentAllocation.create(
                            savedPayment.getId(),
                            installment.getId(),
                            allocationOrder,
                            allocationAmount
                    )
            );

            allocationOrder++;
            remainingToAllocate =
                    remainingToAllocate.subtract(
                            allocationAmount
                    );
        }

        if (remainingToAllocate.signum() != 0) {
            throw new PaymentConflictException(
                    "Payment amount exceeds allocatable "
                            + "installment balance"
            );
        }

        account.recordPayment(paymentAmount);

        List<PaymentAllocation> savedAllocations =
                paymentAllocationRepository
                        .saveAll(allocations);

        return PaymentResponse.from(
                savedPayment,
                account,
                savedAllocations
        );
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(UUID paymentId) {
        Payment payment = paymentRepository
                .findById(paymentId)
                .orElseThrow(
                        () -> new PaymentNotFoundException(
                                paymentId
                        )
                );

        EnrollmentFinancialAccount account =
                financialAccountRepository
                        .findById(
                                payment.getFinancialAccountId()
                        )
                        .orElseThrow(
                                () -> new IllegalStateException(
                                        "Payment financial account "
                                                + "does not exist"
                                )
                        );

        List<PaymentAllocation> allocations =
                paymentAllocationRepository
                        .findAllByPaymentIdOrderByAllocationOrderAsc(
                                paymentId
                        );

        return PaymentResponse.from(
                payment,
                account,
                allocations
        );
    }

    private BigDecimal normalizePaymentAmount(
            BigDecimal amount
    ) {
        try {
            return amount.setScale(
                    2,
                    RoundingMode.UNNECESSARY
            );
        }
        catch (ArithmeticException exception) {
            throw new PaymentConflictException(
                    "Payment amount must have no more "
                            + "than two decimal places"
            );
        }
    }

    private String normalizeExternalReference(
            String externalReference
    ) {
        if (
                externalReference == null
                        || externalReference.isBlank()
        ) {
            return null;
        }

        return externalReference.trim();
    }
}