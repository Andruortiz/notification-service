package co.edu.uco.notification.core.valueobject;

import co.edu.uco.notification.core.exception.InvalidNotificationDataException;

import java.util.Locale;

/**
 * Medio por el que se entrega una notificación: correo, mensaje de texto, mensajería, webhook.
 *
 * <p><strong>Deliberadamente no es un enum.</strong> Convertirlo en un conjunto cerrado obligaría a
 * recompilar y desplegar el componente cada vez que apareciera un canal nuevo, que es justo lo que
 * el diseño quiere evitar. El conjunto de canales válidos vive en el catálogo, no en el código: el
 * dominio solo garantiza que el valor tenga forma de canal, y la validación contra el catálogo
 * ocurre en el caso de uso a través de su puerto.
 */
public record ChannelType(String value) {

    private static final int MAX_LENGTH = 40;

    public ChannelType {
        if (value == null || value.isBlank()) {
            throw new InvalidNotificationDataException("El canal es obligatorio");
        }
        value = value.trim().toUpperCase(Locale.ROOT);
        if (value.length() > MAX_LENGTH) {
            throw new InvalidNotificationDataException(
                    "El canal no puede superar %d caracteres".formatted(MAX_LENGTH));
        }
        if (!value.matches("[A-Z0-9_]+")) {
            throw new InvalidNotificationDataException(
                    "El canal solo admite letras, dígitos y guion bajo: " + value);
        }
    }

    public static ChannelType of(final String value) {
        return new ChannelType(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
