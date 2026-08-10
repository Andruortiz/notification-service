package co.edu.uco.notification.application.command;

import java.util.Map;

/**
 * Solicitud de envío tal como llega a la capa de aplicación, ya libre de detalles de transporte.
 *
 * <p>El contenido viaja como conjunto abierto de datos: la aplicación no sabe qué campos exige
 * cada canal, y por eso admitir un canal nuevo no obliga a cambiar este comando.
 *
 * @param tenantId   sistema cliente que solicita el envío
 * @param externalId referencia del cliente, base de la idempotencia
 * @param channel    canal por el que debe entregarse
 * @param recipient  dirección de destino
 * @param content    datos del mensaje, cuya forma depende del canal
 * @param priority   urgencia declarada; si es nula se aplica la prioridad media
 */
public record SendNotificationCommand(
        String tenantId,
        String externalId,
        String channel,
        String recipient,
        Map<String, Object> content,
        String priority) {
}
