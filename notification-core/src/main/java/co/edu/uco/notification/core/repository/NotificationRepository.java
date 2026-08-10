package co.edu.uco.notification.core.repository;

import co.edu.uco.notification.core.aggregate.Notification;
import co.edu.uco.notification.core.valueobject.ExternalId;
import co.edu.uco.notification.core.valueobject.NotificationId;
import co.edu.uco.notification.core.valueobject.TenantId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Abstracción de persistencia del agregado, expresada en términos del dominio.
 *
 * <p>El dominio declara lo que necesita; la infraestructura decide cómo cumplirlo. La firma usa
 * tipos reactivos porque el componente es no bloqueante de extremo a extremo, pero no menciona
 * MongoDB ni ningún otro almacén.
 */
public interface NotificationRepository {

    Mono<Notification> save(Notification notification);

    Mono<Notification> findById(NotificationId id);

    /** Búsqueda acotada por cliente: es la que sostiene el aislamiento entre sistemas. */
    Mono<Notification> findByTenantAndId(TenantId tenantId, NotificationId id);

    /** Base de la idempotencia: localiza una solicitud ya recibida con la misma referencia. */
    Mono<Notification> findByTenantAndExternalId(TenantId tenantId, ExternalId externalId);

    /** Notificaciones pendientes de despacho, para el reconciliador. */
    Flux<Notification> findPending(int limit);
}
