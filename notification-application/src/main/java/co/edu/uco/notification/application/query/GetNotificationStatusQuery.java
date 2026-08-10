package co.edu.uco.notification.application.query;

/**
 * Consulta del estado de una notificación.
 *
 * <p>Incluye el cliente porque la búsqueda se acota por él: es lo que impide que un sistema
 * consulte notificaciones de otro.
 *
 * @param tenantId       sistema cliente que consulta
 * @param notificationId identificador de seguimiento entregado al aceptar la solicitud
 */
public record GetNotificationStatusQuery(String tenantId, String notificationId) {
}
