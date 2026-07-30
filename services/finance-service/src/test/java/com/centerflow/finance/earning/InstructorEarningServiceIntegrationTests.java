package com.centerflow.finance.earning;

import com.centerflow.finance.earning.api.dto.CancelInstructorEarningRequest;
import com.centerflow.finance.earning.api.dto.CreateInstructorEarningRequest;
import com.centerflow.finance.earning.api.dto.PayInstructorEarningRequest;
import com.centerflow.finance.earning.application.InstructorEarningService;
import com.centerflow.finance.earning.domain.InstructorEarningStatus;
import com.centerflow.finance.earning.exception.InstructorEarningConflictException;
import com.centerflow.finance.earning.repository.InstructorEarningRepository;
import com.centerflow.finance.payment.domain.PaymentMethod;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class InstructorEarningServiceIntegrationTests {

    @Autowired
    private InstructorEarningService earningService;

    @Autowired
    private InstructorEarningRepository earningRepository;

    @Test
    void earningShouldBeRecordedSearchedAndPaid() {
        UUID instructorId = UUID.randomUUID();
        UUID batchId = UUID.randomUUID();

        CreateInstructorEarningRequest request =
                createRequest(
                        instructorId,
                        UUID.randomUUID(),
                        batchId,
                        new BigDecimal("400.00")
                );

        var recorded =
                earningService.recordEarning(request);

        assertThat(recorded.status())
                .isEqualTo(
                        InstructorEarningStatus.ACCRUED
                );

        var searchResult =
                earningService.searchEarnings(
                        instructorId,
                        batchId,
                        InstructorEarningStatus.ACCRUED,
                        LocalDate.now(ZoneOffset.UTC),
                        LocalDate.now(ZoneOffset.UTC),
                        0,
                        10
                );

        assertThat(searchResult.totalElements())
                .isEqualTo(1);

        var paid = earningService.payEarning(
                recorded.id(),
                new PayInstructorEarningRequest(
                        PaymentMethod.BANK_TRANSFER,
                        "PAY-" + UUID.randomUUID()
                )
        );

        assertThat(paid.status())
                .isEqualTo(InstructorEarningStatus.PAID);

        assertThat(paid.paidAt()).isNotNull();
    }

    @Test
    void sameSessionAndSameRequestShouldBeIdempotent() {
        CreateInstructorEarningRequest request =
                createRequest(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        new BigDecimal("350.00")
                );

        var first =
                earningService.recordEarning(request);

        var second =
                earningService.recordEarning(request);

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(earningRepository.count()).isEqualTo(1);
    }

    @Test
    void sameSessionWithDifferentAmountShouldBeRejected() {
        UUID sessionId = UUID.randomUUID();
        UUID instructorId = UUID.randomUUID();
        UUID batchId = UUID.randomUUID();

        earningService.recordEarning(
                createRequest(
                        instructorId,
                        sessionId,
                        batchId,
                        new BigDecimal("400.00")
                )
        );

        assertThatThrownBy(
                () -> earningService.recordEarning(
                        createRequest(
                                instructorId,
                                sessionId,
                                batchId,
                                new BigDecimal("500.00")
                        )
                )
        )
                .isInstanceOf(
                        InstructorEarningConflictException.class
                )
                .hasMessageContaining(
                        "different instructor earning"
                );
    }

    @Test
    void duplicatePaymentReferenceShouldBeRejected() {
        var first = earningService.recordEarning(
                createRequest(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        new BigDecimal("300.00")
                )
        );

        var second = earningService.recordEarning(
                createRequest(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        new BigDecimal("300.00")
                )
        );

        String reference =
                "PAY-" + UUID.randomUUID();

        earningService.payEarning(
                first.id(),
                new PayInstructorEarningRequest(
                        PaymentMethod.CASH,
                        reference
                )
        );

        assertThatThrownBy(
                () -> earningService.payEarning(
                        second.id(),
                        new PayInstructorEarningRequest(
                                PaymentMethod.CASH,
                                reference
                        )
                )
        )
                .isInstanceOf(
                        InstructorEarningConflictException.class
                )
                .hasMessageContaining(reference);
    }

    @Test
    void accruedEarningShouldBeCancelled() {
        var recorded = earningService.recordEarning(
                createRequest(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        new BigDecimal("250.00")
                )
        );

        var cancelled =
                earningService.cancelEarning(
                        recorded.id(),
                        new CancelInstructorEarningRequest(
                                "Session was cancelled"
                        )
                );

        assertThat(cancelled.status())
                .isEqualTo(
                        InstructorEarningStatus.CANCELLED
                );

        assertThat(cancelled.cancelledAt())
                .isNotNull();
    }

    private CreateInstructorEarningRequest createRequest(
            UUID instructorId,
            UUID sessionId,
            UUID batchId,
            BigDecimal amount
    ) {
        return new CreateInstructorEarningRequest(
                instructorId,
                sessionId,
                batchId,
                amount,
                "EGP",
                LocalDate.now(ZoneOffset.UTC),
                "Completed instructor session"
        );
    }
}