package com.centerflow.finance.account.scheduling;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

@Configuration
@EnableScheduling
@Profile("!test")
public class FinanceSchedulingConfiguration {

    @Bean
    public Clock financeClock() {
        return Clock.systemUTC();
    }
}