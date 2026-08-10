package co.edu.uco.notification.core.event;

import co.edu.uco.notification.core.valueobject.ChannelType;
import co.edu.uco.notification.core.valueobject.NotificationId;
import co.edu.uco.notification.core.valueobject.Priority;

import java.time.Instant;

/** La notificación se publicó en la cola de despacho con su prioridad. */
public record NotificationQueued(
        NotificationId notificationId,
        ChannelType channel,
        Priority priority,
        Instant occurredOn) implements DomainEvent {
}
