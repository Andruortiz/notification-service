package co.edu.uco.notification.core.event;

import co.edu.uco.notification.core.valueobject.ChannelType;
import co.edu.uco.notification.core.valueobject.NotificationId;
import co.edu.uco.notification.core.valueobject.Priority;
import co.edu.uco.notification.core.valueobject.TenantId;

import java.time.Instant;

/** La solicitud superó la validación y quedó registrada. */
public record NotificationAccepted(
        NotificationId notificationId,
        TenantId tenantId,
        ChannelType channel,
        Priority priority,
        Instant occurredOn) implements DomainEvent {
}
