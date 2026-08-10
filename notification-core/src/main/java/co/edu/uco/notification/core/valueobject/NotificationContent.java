package co.edu.uco.notification.core.valueobject;

import co.edu.uco.notification.core.exception.InvalidNotificationDataException;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Contenido de la notificación como conjunto abierto de datos.
 *
 * <p>El dominio transporta estos datos sin interpretarlos: no sabe que un correo lleva asunto y
 * cuerpo, ni que un mensaje de texto lleva solo texto. Esa diferencia la describe el esquema del
 * canal en el catálogo, lo que permite dar de alta canales nuevos sin tocar el núcleo.
 */
public record NotificationContent(Map<String, Object> data) {

    public NotificationContent {
        if (data == null || data.isEmpty()) {
            throw new InvalidNotificationDataException("El contenido de la notificación es obligatorio");
        }
        data = Collections.unmodifiableMap(new LinkedHashMap<>(data));
    }

    public static NotificationContent of(final Map<String, Object> data) {
        return new NotificationContent(data);
    }

    /** Devuelve un campo del contenido como texto, si está presente. */
    public Optional<String> text(final String key) {
        return Optional.ofNullable(data.get(key)).map(Object::toString);
    }

    public boolean contains(final String key) {
        return data.containsKey(key);
    }
}
