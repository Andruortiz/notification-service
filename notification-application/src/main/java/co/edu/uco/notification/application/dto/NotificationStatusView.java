package co.edu.uco.notification.application.dto;

import java.time.Instant;
import java.util.List;

/**
 * Vista de solo lectura del estado y la trazabilidad de una notificación.
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
public record NotificationStatusView(
        String notificationId,
        String tenantId,
        String channel,
        String recipient,
        String status,
        String priority,
        Instant createdAt,
        Instant updatedAt,
        List<AttemptView> attempts) {

    /**
     * Intento de envío tal como se expone hacia el exterior.
     *
     * @param number      número de intento
     * @param providerId  proveedor que lo ejecutó
     * @param outcome     resultado del intento
     * @param detail      código o mensaje devuelto por el proveedor
     * @param attemptedAt momento del intento
     */
    public record AttemptView(
            int number,
            String providerId,
            String outcome,
            String detail,
            Instant attemptedAt) {
    }
}
