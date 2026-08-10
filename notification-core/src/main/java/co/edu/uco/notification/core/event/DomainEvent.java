package co.edu.uco.notification.core.event;

import co.edu.uco.notification.core.valueobject.NotificationId;

import java.time.Instant;

/**
 * Hecho relevante ocurrido dentro del dominio.
 *
 * <p>El agregado los acumula y la capa de aplicación los recoge para publicarlos. El dominio no
 * conoce el mecanismo de publicación: no sabe si acabarán en una cola, en un registro de auditoría
 * o en ambos.
 */
public sealed interface DomainEvent
        permits NotificationAccepted, NotificationQueued, NotificationDelivered, NotificationFailed {

    NotificationId notificationId();

    Instant occurredOn();
}
