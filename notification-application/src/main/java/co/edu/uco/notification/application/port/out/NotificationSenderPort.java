package co.edu.uco.notification.application.port.out;

import co.edu.uco.notification.core.aggregate.Notification;
import reactor.core.publisher.Mono;

/**
 * Entrega efectiva de la notificación a través del proveedor que corresponda.
 *
 * <p>La aplicación no conoce ningún proveedor concreto: le pasa el agregado y la ruta resuelta, y
 * recibe un resultado normalizado. Integrar un proveedor nuevo consiste en escribir un adaptador
 * que participe detrás de este puerto.
 */
public interface NotificationSenderPort {

    Mono<SendOutcome> send(Notification notification, ChannelCatalogPort.ChannelRoute route);

    /**
     * Resultado normalizado de un envío.
     *
     * <p>La clasificación entre recuperable y definitivo la hace el adaptador, que es quien conoce
     * los códigos del proveedor. El caso de uso solo decide qué hacer con ella.
     *
     * @param accepted          cierto si el proveedor aceptó el envío
     * @param providerId        proveedor que atendió el intento
     * @param providerMessageId identificador devuelto por el proveedor, si lo hay
     * @param detail            código o mensaje del proveedor
     * @param recoverable       cierto si el fallo admite reintento
     */
    record SendOutcome(
            boolean accepted,
            String providerId,
            String providerMessageId,
            String detail,
            boolean recoverable) {

        public static SendOutcome accepted(final String providerId, final String messageId) {
            return new SendOutcome(true, providerId, messageId, "OK", false);
        }

        public static SendOutcome recoverableFailure(final String providerId, final String detail) {
            return new SendOutcome(false, providerId, null, detail, true);
        }

        public static SendOutcome permanentFailure(final String providerId, final String detail) {
            return new SendOutcome(false, providerId, null, detail, false);
        }
    }
}
