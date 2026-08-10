package co.edu.uco.notification.adapters.out.rabbit;

/**
 * Mensaje que viaja por la cola de despacho.
 *
 * <p>Lleva solo el identificador y los datos mínimos de enrutamiento, no la notificación entera:
 * el despachador la relee de la base de datos para trabajar siempre con su estado actual y no con
 * una copia que pudo quedar obsoleta en la cola.
 *
 * @param notificationId identificador de la notificación a despachar
 * @param channel        canal, útil para inspeccionar la cola sin consultar la base de datos
 * @param priority       peso de prioridad con el que se publicó
 */
public record NotificationDispatchMessage(String notificationId, String channel, int priority) {
}
