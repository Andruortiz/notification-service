package co.edu.uco.notification.application.port.in;

import reactor.core.publisher.Mono;

/**
 * Puerto de entrada del despacho: toma una notificación ya aceptada y ejecuta su envío.
 *
 * <p>Lo invoca el consumidor de la cola, y también el reconciliador cuando reencola una
 * notificación que quedó pendiente. Separarlo de la aceptación es lo que permite que la respuesta
 * al cliente no dependa de la latencia del proveedor.
 */
public interface DispatchNotificationUseCase {

    Mono<Void> dispatch(String notificationId);
}
