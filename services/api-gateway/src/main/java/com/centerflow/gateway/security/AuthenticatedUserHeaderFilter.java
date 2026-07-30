package com.centerflow.gateway.security;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AuthenticatedUserHeaderFilter
        implements GlobalFilter, Ordered {

    public static final String USER_ID_HEADER =
            "X-User-Id";

    private static final String NOTIFICATION_API_PREFIX =
            "/api/v1/notifications";

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain
    ) {
        String requestPath =
                exchange.getRequest()
                        .getURI()
                        .getPath();

        if (!isNotificationRequest(requestPath)) {
            return chain.filter(exchange);
        }

        return exchange.getPrincipal()
                .ofType(JwtAuthenticationToken.class)
                .flatMap(authentication -> {
                    String userId =
                            authentication
                                    .getToken()
                                    .getSubject();

                    if (
                            userId == null
                                    || userId.isBlank()
                    ) {
                        return chain.filter(exchange);
                    }

                    var updatedRequest =
                            exchange.getRequest()
                                    .mutate()
                                    .headers(headers -> {
                                        headers.remove(
                                                USER_ID_HEADER
                                        );

                                        headers.set(
                                                USER_ID_HEADER,
                                                userId
                                        );
                                    })
                                    .build();

                    var updatedExchange =
                            exchange.mutate()
                                    .request(updatedRequest)
                                    .build();

                    return chain.filter(updatedExchange);
                })
                .switchIfEmpty(
                        chain.filter(exchange)
                );
    }

    private boolean isNotificationRequest(
            String requestPath
    ) {
        return requestPath.equals(
                NOTIFICATION_API_PREFIX
        )
                || requestPath.startsWith(
                        NOTIFICATION_API_PREFIX + "/"
                );
    }

    @Override
    public int getOrder() {
        return -100;
    }
}