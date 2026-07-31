package com.centerflow.finance.account.scheduling;

import com.centerflow.finance.account.api.dto.OverdueProcessingResponse;
import com.centerflow.finance.account.application.InstallmentCollectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

@Component
@Profile("!test")
@ConditionalOnProperty(
        prefix = "centerflow.jobs.overdue-installments",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class OverdueInstallmentScheduler {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(
                    OverdueInstallmentScheduler.class
            );

    private final InstallmentCollectionService
            installmentCollectionService;

    private final Clock financeClock;

    public OverdueInstallmentScheduler(
            InstallmentCollectionService
                    installmentCollectionService,
            Clock financeClock
    ) {
        this.installmentCollectionService =
                installmentCollectionService;

        this.financeClock = financeClock;
    }

    @Scheduled(
            cron = "${centerflow.jobs.overdue-installments.cron:0 5 0 * * *}",
            zone = "${centerflow.jobs.overdue-installments.zone:UTC}"
    )
    public void processOverdueInstallments() {
        LocalDate asOfDate =
                LocalDate.now(financeClock);

        OverdueProcessingResponse response =
                installmentCollectionService
                        .markOverdueInstallments(
                                asOfDate
                        );

        if (response.markedOverdueCount() > 0) {
            LOGGER.info(
                    "Marked {} installments overdue as of {}",
                    response.markedOverdueCount(),
                    response.asOfDate()
            );
        }
    }
}