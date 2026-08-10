package co.edu.uco.notification.adapters.in.rest.response;

import java.time.Instant;

/**
 * Respuesta a una solicitud de envío aceptada.
 *
 * @param notificationId identificador de seguimiento con el que consultar el estado
 * @param status         estado en que quedó la notificación
 * @param duplicate      cierto si la referencia externa ya se había recibido
 * @param acceptedAt     momento de la aceptación
 */
public record NotificationAcceptedResponse(
        String notificationId,
        String status,
        boolean duplicate,
        Instant acceptedAt) {
}
