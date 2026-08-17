package co.edu.uco.notification.adapters.in.rest.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * Cuerpo de la solicitud de envío.
 *
 * <p>Las restricciones declaradas aquí son las estructurales, comunes a todos los canales. Las que
 * dependen del canal —que un correo traiga asunto, que un mensaje de texto no exceda su longitud—
 * no se validan con anotaciones: las describe el esquema del canal en el catálogo, y por eso
 * {@code content} es un mapa abierto y no una clase con campos fijos.
 *
 * @param externalId referencia del sistema cliente, base de la idempotencia
 * @param channel    canal solicitado
 * @param recipient  dirección de destino
 * @param content    datos del mensaje, con la forma que exija el canal
 * @param priority   urgencia declarada; opcional
 */
public record SendNotificationRequest(

        @NotBlank(message = "La referencia externa es obligatoria")
        @Size(max = 120, message = "La referencia externa no puede superar 120 caracteres")
        String externalId,

        @NotBlank(message = "El canal es obligatorio")
        @Size(max = 40, message = "El canal no puede superar 40 caracteres")
        String channel,

        @NotBlank(message = "El destinatario es obligatorio")
        @Size(max = 512, message = "El destinatario no puede superar 512 caracteres")
        String recipient,

        @NotEmpty(message = "El contenido de la notificación es obligatorio")
        Map<String, Object> content,

        String priority) {

        public SendNotificationRequest {
                content = content == null ? Map.of() : Map.copyOf(content);
        }
}
