package co.edu.uco.notification.adapters.in.rest.response;

import java.time.Instant;
import java.util.List;

/**
 * Detalle de una notificación con su trazabilidad.
 *
 * @param notificationId identificador de seguimiento
 * @param tenantId       sistema cliente propietario
 * @param channel        canal por el que se entrega
 * @param recipient      dirección de destino
 * @param status         estado actual
 * @param priority       urgencia declarada
 * @param createdAt      momento de la aceptación
 * @param updatedAt      momento del último cambio de estado
 * @param attempts       intentos registrados, en orden cronológico
 */
public record NotificationDetailResponse(
        String notificationId,
        String tenantId,
        String channel,
        String recipient,
        String status,
        String priority,
        Instant createdAt,
        Instant updatedAt,
        List<AttemptResponse> attempts) {

    public NotificationDetailResponse {
        attempts = attempts == null ? List.of() : List.copyOf(attempts);
    }

    /**
     * Intento de envío expuesto hacia el exterior.
     *
     * @param number      número de intento
     * @param providerId  proveedor que lo ejecutó
     * @param outcome     resultado del intento
     * @param detail      código o mensaje devuelto por el proveedor
     * @param attemptedAt momento del intento
     */
    public record AttemptResponse(
            int number,
            String providerId,
            String outcome,
            String detail,
            Instant attemptedAt) {
    }
}
