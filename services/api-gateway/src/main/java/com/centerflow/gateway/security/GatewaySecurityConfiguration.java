package com.centerflow.gateway.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtGrantedAuthoritiesConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration(proxyBeanMethods = false)
@EnableWebFluxSecurity
public class GatewaySecurityConfiguration {

    private static final String ADMIN =
            "ADMIN";

    private static final String BRANCH_MANAGER =
            "BRANCH_MANAGER";

    private static final String ACCOUNTANT =
            "ACCOUNTANT";

    private static final String INSTRUCTOR =
            "INSTRUCTOR";

    private static final String RECEPTIONIST =
            "RECEPTIONIST";

    private static final String STUDENT =
            "STUDENT";

    @Bean
    ReactiveJwtAuthenticationConverter
    gatewayJwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter
                authoritiesConverter =
                new JwtGrantedAuthoritiesConverter();

        authoritiesConverter.setAuthoritiesClaimName(
                "roles"
        );

        authoritiesConverter.setAuthorityPrefix(
                "ROLE_"
        );

        ReactiveJwtAuthenticationConverter
                authenticationConverter =
                new ReactiveJwtAuthenticationConverter();

        authenticationConverter
                .setJwtGrantedAuthoritiesConverter(
                        new ReactiveJwtGrantedAuthoritiesConverterAdapter(
                                authoritiesConverter
                        )
                );

        return authenticationConverter;
    }

    @Bean
    SecurityWebFilterChain gatewaySecurityFilterChain(
            ServerHttpSecurity http,
            ReactiveJwtAuthenticationConverter
                    gatewayJwtAuthenticationConverter
    ) {
        http
                .csrf(
                        ServerHttpSecurity.CsrfSpec::disable
                )
                .formLogin(
                        ServerHttpSecurity
                                .FormLoginSpec::disable
                )
                .httpBasic(
                        ServerHttpSecurity
                                .HttpBasicSpec::disable
                )
                .logout(
                        ServerHttpSecurity
                                .LogoutSpec::disable
                )

                .authorizeExchange(authorize ->
                        authorize
                                .pathMatchers(
                                        "/api/v1/academic/internal/**",
                                        "/api/v1/enrollments/internal/**",
                                        "/api/v1/finance/internal/**",
                                        "/api/v1/notifications/internal/**"
                                )
                                .denyAll()

                                .pathMatchers(
                                        HttpMethod.POST,
                                        "/api/v1/auth/register",
                                        "/api/v1/auth/login",
                                        "/api/v1/auth/refresh",
                                        "/api/v1/auth/logout",
                                        "/api/v1/auth/password/forgot",
                                        "/api/v1/auth/password/reset"
                                )
                                .permitAll()

                                .pathMatchers(
                                        "/actuator/health",
                                        "/actuator/health/**",
                                        "/actuator/info",
                                        "/actuator/prometheus"
                                )
                                .permitAll()

                                .pathMatchers(
                                        "/api/v1/notifications/**"
                                )
                                .authenticated()

                                .pathMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/academic/reports/**"
                                )
                                .hasAnyRole(
                                        ADMIN,
                                        BRANCH_MANAGER,
                                        INSTRUCTOR
                                )

                                .pathMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/academic/batch-sessions/*/attendance",
                                        "/api/v1/academic/batch-sessions/*/attendance/summary",
                                        "/api/v1/academic/batches/*/attendance"
                                )
                                .hasAnyRole(
                                        ADMIN,
                                        BRANCH_MANAGER,
                                        INSTRUCTOR
                                )

                                .pathMatchers(
                                        HttpMethod.PUT,
                                        "/api/v1/academic/batch-sessions/*/attendance"
                                )
                                .hasAnyRole(
                                        ADMIN,
                                        BRANCH_MANAGER,
                                        INSTRUCTOR
                                )

                                .pathMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/academic/**"
                                )
                                .hasAnyRole(
                                        ADMIN,
                                        BRANCH_MANAGER,
                                        ACCOUNTANT,
                                        INSTRUCTOR,
                                        RECEPTIONIST,
                                        STUDENT
                                )

                                .pathMatchers(
                                        HttpMethod.POST,
                                        "/api/v1/academic/**"
                                )
                                .hasAnyRole(
                                        ADMIN,
                                        BRANCH_MANAGER
                                )

                                .pathMatchers(
                                        HttpMethod.PUT,
                                        "/api/v1/academic/**"
                                )
                                .hasAnyRole(
                                        ADMIN,
                                        BRANCH_MANAGER
                                )

                                .pathMatchers(
                                        HttpMethod.PATCH,
                                        "/api/v1/academic/**"
                                )
                                .hasAnyRole(
                                        ADMIN,
                                        BRANCH_MANAGER
                                )

                                .pathMatchers(
                                        HttpMethod.DELETE,
                                        "/api/v1/academic/**"
                                )
                                .hasAnyRole(
                                        ADMIN,
                                        BRANCH_MANAGER
                                )

                                .pathMatchers(
                                        "/api/v1/enrollments/**"
                                )
                                .hasAnyRole(
                                        ADMIN,
                                        BRANCH_MANAGER,
                                        RECEPTIONIST
                                )

                                .pathMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/finance/reports/**"
                                )
                                .hasAnyRole(
                                        ADMIN,
                                        ACCOUNTANT,
                                        BRANCH_MANAGER
                                )

                                .pathMatchers(
                                        "/api/v1/finance/**"
                                )
                                .hasAnyRole(
                                        ADMIN,
                                        ACCOUNTANT
                                )

                                .pathMatchers(
                                        "/api/v1/auth/admin/**"
                                )
                                .hasRole(ADMIN)

                                .anyExchange()
                                .authenticated()
                )

                .oauth2ResourceServer(resourceServer ->
                        resourceServer.jwt(jwt ->
                                jwt.jwtAuthenticationConverter(
                                        gatewayJwtAuthenticationConverter
                                )
                        )
                );

        return http.build();
    }
}