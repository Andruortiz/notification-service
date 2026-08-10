package co.edu.uco.notification.core.event;

import co.edu.uco.notification.core.valueobject.NotificationId;

import java.time.Instant;

/** El envío falló de forma definitiva y la notificación quedó aislada. */
public record NotificationFailed(
        NotificationId notificationId,
        String reason,
        int attempts,
        Instant occurredOn) implements DomainEvent {
}
