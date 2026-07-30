package com.centerflow.finance.integration.notification;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class FinanceNotificationEventListenerTests {

    @Test
    void listenerShouldForwardEventToClient() {
        FinanceNotificationClient client =
                mock(FinanceNotificationClient.class);

        FinanceNotificationEventListener listener =
                new FinanceNotificationEventListener(
                        client
                );

        FinanceNotificationEvent event =
                createEvent();

        listener.handle(event);

        verify(client).createNotification(event);
    }

    @Test
    void notificationFailureShouldNotFailFinanceOperation() {
        FinanceNotificationClient client =
                mock(FinanceNotificationClient.class);

        FinanceNotificationEventListener listener =
                new FinanceNotificationEventListener(
                        client
                );

        FinanceNotificationEvent event =
                createEvent();

        doThrow(
                new IllegalStateException(
                        "Notification Service unavailable"
                )
        )
                .when(client)
                .createNotification(event);

        assertThatCode(
                () -> listener.handle(event)
        ).doesNotThrowAnyException();
    }

    private FinanceNotificationEvent createEvent() {
        return new FinanceNotificationEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                FinanceNotificationType
                        .PAYMENT_RECORDED,
                "Payment received",
                "Payment was recorded successfully.",
                "ENROLLMENT",
                UUID.randomUUID()
        );
    }
}