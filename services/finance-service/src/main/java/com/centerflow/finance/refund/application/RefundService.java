package com.centerflow.finance.refund.application;

import com.centerflow.finance.account.domain.EnrollmentFinancialAccount;
import com.centerflow.finance.account.domain.Installment;
import com.centerflow.finance.account.repository.EnrollmentFinancialAccountRepository;
import com.centerflow.finance.account.repository.InstallmentRepository;
import com.centerflow.finance.payment.domain.Payment;
import com.centerflow.finance.payment.domain.PaymentAllocation;
import com.centerflow.finance.payment.exception.PaymentNotFoundException;
import com.centerflow.finance.payment.repository.PaymentAllocationRepository;
import com.centerflow.finance.payment.repository.PaymentRepository;
import com.centerflow.finance.refund.api.dto.RecordRefundRequest;
import com.centerflow.finance.refund.api.dto.RefundResponse;
import com.centerflow.finance.refund.domain.Refund;
import com.centerflow.finance.refund.domain.RefundAllocation;
import com.centerflow.finance.refund.exception.RefundConflictException;
import com.centerflow.finance.refund.exception.RefundNotFoundException;
import com.centerflow.finance.refund.number.RefundNumberGenerator;
import com.centerflow.finance.refund.repository.RefundAllocationRepository;
import com.centerflow.finance.refund.repository.RefundRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class RefundService {

    private final RefundRepository refundRepository;
    private final RefundAllocationRepository
            refundAllocationRepository;

    private final PaymentRepository paymentRepository;
    private final PaymentAllocationRepository
            paymentAllocationRepository;

    private final EnrollmentFinancialAccountRepository
            financialAccountRepository;

    private final InstallmentRepository
            installmentRepository;

    private final RefundNumberGenerator
            refundNumberGenerator;

    public RefundService(
            RefundRepository refundRepository,
            RefundAllocationRepository
                    refundAllocationRepository,
            PaymentRepository paymentRepository,
            PaymentAllocationRepository
                    paymentAllocationRepository,
            EnrollmentFinancialAccountRepository
                    financialAccountRepository,
            InstallmentRepository installmentRepository,
            RefundNumberGenerator refundNumberGenerator
    ) {
        this.refundRepository = refundRepository;
        this.refundAllocationRepository =
                refundAllocationRepository;
        this.paymentRepository = paymentRepository;
        this.paymentAllocationRepository =
                paymentAllocationRepository;
        this.financialAccountRepository =
                financialAccountRepository;
        this.installmentRepository =
                installmentRepository;
        this.refundNumberGenerator =
                refundNumberGenerator;
    }

    @Transactional
    public RefundResponse recordRefund(
            UUID paymentId,
            RecordRefundRequest request
    ) {
        Payment payment = paymentRepository
                .findByIdForUpdate(paymentId)
                .orElseThrow(
                        () -> new PaymentNotFoundException(
                                paymentId
                        )
                );

        BigDecimal refundAmount =
                normalizeRefundAmount(request.amount());

        String externalReference =
                normalizeExternalReference(
                        request.externalReference()
                );

        if (
                externalReference != null
                        && refundRepository
                        .existsByExternalReference(
                                externalReference
                        )
        ) {
            throw new RefundConflictException(
                    "Refund external reference "
                            + "already exists: "
                            + externalReference
            );
        }

        if (
                refundAmount.compareTo(
                        payment.getRefundableAmount()
                ) > 0
        ) {
            throw new RefundConflictException(
                    "Refund amount exceeds payment "
                            + "refundable amount"
            );
        }

        EnrollmentFinancialAccount account =
                financialAccountRepository
                        .findByIdForUpdate(
                                payment
                                        .getFinancialAccountId()
                        )
                        .orElseThrow(
                                () -> new IllegalStateException(
                                        "Payment financial account "
                                                + "does not exist"
                                )
                        );

        List<PaymentAllocation> paymentAllocations =
                paymentAllocationRepository
                        .findAllByPaymentIdForRefund(
                                paymentId
                        );

        Refund refund = Refund.create(
                refundNumberGenerator.nextNumber(),
                payment.getId(),
                refundAmount,
                payment.getCurrency(),
                request.reason(),
                externalReference
        );

        Refund savedRefund =
                refundRepository.saveAndFlush(refund);

        BigDecimal remainingToRefund = refundAmount;

        List<RefundAllocation> refundAllocations =
                new ArrayList<>();

        int refundOrder = 1;

        for (
                PaymentAllocation paymentAllocation
                : paymentAllocations
        ) {
            if (remainingToRefund.signum() == 0) {
                break;
            }

            BigDecimal allocationRefundable =
                    paymentAllocation
                            .getRefundableAmount();

            if (allocationRefundable.signum() == 0) {
                continue;
            }

            BigDecimal allocationRefund =
                    remainingToRefund.min(
                            allocationRefundable
                    );

            Installment installment =
                    installmentRepository
                            .findByIdForUpdate(
                                    paymentAllocation
                                            .getInstallmentId()
                            )
                            .orElseThrow(
                                    () -> new IllegalStateException(
                                            "Payment installment "
                                                    + "does not exist"
                                    )
                            );

            installment.refundPayment(
                    allocationRefund
            );

            paymentAllocation.recordRefund(
                    allocationRefund
            );

            refundAllocations.add(
                    RefundAllocation.create(
                            savedRefund.getId(),
                            paymentAllocation.getId(),
                            installment.getId(),
                            refundOrder,
                            allocationRefund
                    )
            );

            refundOrder++;
            remainingToRefund =
                    remainingToRefund.subtract(
                            allocationRefund
                    );
        }

        if (remainingToRefund.signum() != 0) {
            throw new RefundConflictException(
                    "Refund amount exceeds refundable "
                            + "payment allocations"
            );
        }

        payment.recordRefund(refundAmount);
        account.recordRefund(refundAmount);

        List<RefundAllocation> savedAllocations =
                refundAllocationRepository
                        .saveAll(refundAllocations)
                        .stream()
                        .sorted(
                                Comparator.comparingInt(
                                        RefundAllocation
                                                ::getAllocationOrder
                                )
                        )
                        .toList();

        return RefundResponse.from(
                savedRefund,
                payment,
                account,
                savedAllocations
        );
    }

    @Transactional(readOnly = true)
    public RefundResponse getRefund(UUID refundId) {
        Refund refund = refundRepository
                .findById(refundId)
                .orElseThrow(
                        () -> new RefundNotFoundException(
                                refundId
                        )
                );

        Payment payment = paymentRepository
                .findById(refund.getPaymentId())
                .orElseThrow(
                        () -> new IllegalStateException(
                                "Refund payment does not exist"
                        )
                );

        EnrollmentFinancialAccount account =
                financialAccountRepository
                        .findById(
                                payment.getFinancialAccountId()
                        )
                        .orElseThrow(
                                () -> new IllegalStateException(
                                        "Refund financial account "
                                                + "does not exist"
                                )
                        );

        List<RefundAllocation> allocations =
                refundAllocationRepository
                        .findAllByRefundIdOrderByAllocationOrderAsc(
                                refundId
                        );

        return RefundResponse.from(
                refund,
                payment,
                account,
                allocations
        );
    }

    private BigDecimal normalizeRefundAmount(
            BigDecimal amount
    ) {
        try {
            return amount.setScale(
                    2,
                    RoundingMode.UNNECESSARY
            );
        }
        catch (ArithmeticException exception) {
            throw new RefundConflictException(
                    "Refund amount must have no more "
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