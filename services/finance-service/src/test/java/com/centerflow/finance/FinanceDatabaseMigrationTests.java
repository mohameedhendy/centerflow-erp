package com.centerflow.finance;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class FinanceDatabaseMigrationTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayShouldCreateFinanceTables() {
        Integer tableCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name IN (
                      'pricing_plans',
                      'enrollment_financial_accounts',
                      'installments',
                      'payments',
                      'payment_allocations',
                      'refunds',
                      'refund_allocations',
                      'enrollment_activation_tasks',
                      'financial_adjustments',
                      'expenses'
                  )
                """,
                Integer.class
        );

        assertThat(tableCount).isEqualTo(10);
    }

    @Test
    void flywayShouldApplyLatestMigration() {
        String version = jdbcTemplate.queryForObject(
                """
                SELECT version
                FROM flyway_schema_history
                WHERE success = TRUE
                ORDER BY installed_rank DESC
                LIMIT 1
                """,
                String.class
        );

        assertThat(version).isEqualTo("7");
    }
}