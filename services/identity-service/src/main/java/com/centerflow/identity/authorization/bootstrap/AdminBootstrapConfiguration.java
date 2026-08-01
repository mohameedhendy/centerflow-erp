package com.centerflow.identity.authorization.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(
        AdminBootstrapProperties.class
)
public class AdminBootstrapConfiguration {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(
                    AdminBootstrapConfiguration.class
            );

    @Bean
    @ConditionalOnProperty(
            prefix = "security.bootstrap-admin",
            name = "enabled",
            havingValue = "true"
    )
    ApplicationRunner adminBootstrapRunner(
            AdminBootstrapService bootstrapService,
            AdminBootstrapProperties properties
    ) {
        return arguments -> {
            AdminBootstrapResult result =
                    bootstrapService.bootstrap(
                            properties.email(),
                            properties.password()
                    );

            LOGGER.info(
                    "Initial administrator bootstrap result: {}",
                    result
            );
        };
    }
}