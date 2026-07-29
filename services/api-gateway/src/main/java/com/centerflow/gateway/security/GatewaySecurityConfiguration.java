package com.centerflow.gateway.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration(proxyBeanMethods = false)
@EnableWebFluxSecurity
public class GatewaySecurityConfiguration {

    @Bean
    SecurityWebFilterChain gatewaySecurityFilterChain(
            ServerHttpSecurity http
    ) {
        http
                .csrf(
                        ServerHttpSecurity.CsrfSpec::disable
                )
                .formLogin(
                        ServerHttpSecurity.FormLoginSpec::disable
                )
                .httpBasic(
                        ServerHttpSecurity.HttpBasicSpec::disable
                )
                .logout(
                        ServerHttpSecurity.LogoutSpec::disable
                )
                .authorizeExchange(authorize ->
                        authorize
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
                                        "/actuator/info"
                                )
                                .permitAll()

                                .pathMatchers(
                                        "/api/v1/academic/internal/**",
                                        "/api/v1/enrollments/internal/**"
                                )
                                .denyAll()

                                .pathMatchers(
                                        "/api/v1/academic/**",
                                        "/api/v1/enrollments/**"
                                )
                                .authenticated()

                                .anyExchange()
                                .authenticated()
                )
                .oauth2ResourceServer(resourceServer ->
                        resourceServer.jwt(
                                Customizer.withDefaults()
                        )
                );

        return http.build();
    }
}