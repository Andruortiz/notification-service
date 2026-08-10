package co.edu.uco.notification.application.port.out;

import co.edu.uco.notification.core.aggregate.Notification;
import co.edu.uco.notification.core.event.DomainEvent;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Publicación de los eventos de dominio hacia el exterior.
 *
 * <p>La aplicación no sabe si acabarán en una cola, en un registro de auditoría o en ambos: solo
 * sabe que deben salir del proceso.
 */
public interface NotificationEventPublisherPort {

    /** Publica los eventos acumulados por el agregado. */
    Mono<Void> publish(List<DomainEvent> events);

    /**
     * Encola la notificación para su despacho, respetando su prioridad.
     *
     * <p>Se separa de {@link #publish(List)} porque no es un aviso informativo sino la orden de
     * trabajo que dispara el envío, y su garantía de entrega es distinta.
     */
    Mono<Void> enqueueForDispatch(Notification notification);
}
