package co.edu.uco.notification.application.dto;

import java.time.Instant;

/**
 * Resultado de aceptar una solicitud de envío.
 *
 * <p>{@code duplicate} distingue una solicitud nueva de la repetición de una anterior con la misma
 * referencia externa. En ambos casos la respuesta es correcta y devuelve el mismo identificador de
 * seguimiento; el indicador permite al cliente saber que no se generó un segundo envío.
 *
 * @param notificationId identificador de seguimiento
 * @param status         estado en que quedó la notificación
 * @param duplicate      cierto si la solicitud repetía una referencia externa ya recibida
 * @param acceptedAt     momento de la aceptación
 */
public record SendNotificationResult(
        String notificationId,
        String status,
        boolean duplicate,
        Instant acceptedAt) {

    public static SendNotificationResult accepted(
            final String notificationId, final String status, final Instant acceptedAt) {
        return new SendNotificationResult(notificationId, status, false, acceptedAt);
    }

    public static SendNotificationResult duplicated(
            final String notificationId, final String status, final Instant acceptedAt) {
        return new SendNotificationResult(notificationId, status, true, acceptedAt);
    }
}
