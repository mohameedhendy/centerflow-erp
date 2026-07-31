package com.centerflow.finance.account.scheduling;

import com.centerflow.finance.account.api.dto.OverdueProcessingResponse;
import com.centerflow.finance.account.application.InstallmentCollectionService;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OverdueInstallmentSchedulerTests {

    @Test
    void schedulerShouldUseCurrentUtcAccountingDate()
            throws Exception {
        InstallmentCollectionService service =
                mock(
                        InstallmentCollectionService.class
                );

        LocalDate expectedDate =
                LocalDate.of(
                        2026,
                        7,
                        31
                );

        when(
                service.markOverdueInstallments(
                        expectedDate
                )
        ).thenReturn(
                new OverdueProcessingResponse(
                        expectedDate,
                        0
                )
        );

        Clock fixedClock =
                Clock.fixed(
                        Instant.parse(
                                "2026-07-31T23:59:00Z"
                        ),
                        ZoneOffset.UTC
                );

        OverdueInstallmentScheduler scheduler =
                new OverdueInstallmentScheduler(
                        service,
                        fixedClock
                );

        scheduler.processOverdueInstallments();

        verify(service)
                .markOverdueInstallments(
                        expectedDate
                );

        Method scheduledMethod =
                OverdueInstallmentScheduler.class
                        .getDeclaredMethod(
                                "processOverdueInstallments"
                        );

        Scheduled scheduled =
                scheduledMethod.getAnnotation(
                        Scheduled.class
                );

        assertThat(scheduled).isNotNull();

        assertThat(scheduled.cron())
                .isEqualTo(
                        "${centerflow.jobs.overdue-installments.cron:0 5 0 * * *}"
                );

        assertThat(scheduled.zone())
                .isEqualTo(
                        "${centerflow.jobs.overdue-installments.zone:UTC}"
                );
    }
}