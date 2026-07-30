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
    void flywayShouldCreatePricingPlansTable() {
        Integer tableCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name = 'pricing_plans'
                """,
                Integer.class
        );

        assertThat(tableCount).isEqualTo(1);
    }

    @Test
    void flywayShouldApplyVersionOneMigration() {
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

        assertThat(version).isEqualTo("1");
    }
}