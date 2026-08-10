package co.edu.uco.notification.core.event;

import co.edu.uco.notification.core.valueobject.NotificationId;

import java.time.Instant;

/** El proveedor confirmó la aceptación del envío. */
public record NotificationDelivered(
        NotificationId notificationId,
        String providerId,
        String providerMessageId,
        Instant occurredOn) implements DomainEvent {
}
