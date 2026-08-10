package co.edu.uco.notification.core.valueobject;

import co.edu.uco.notification.core.exception.InvalidNotificationDataException;

/**
 * Dirección técnica de destino: un correo, un número, un token de dispositivo o una URL.
 *
 * <p>El dominio no valida el formato concreto porque depende del canal, y los canales son
 * abiertos. Esa validación es declarativa y la resuelve el esquema del canal en el catálogo.
 */
public record Recipient(String value) {

    private static final int MAX_LENGTH = 512;

    public Recipient {
        if (value == null || value.isBlank()) {
            throw new InvalidNotificationDataException("El destinatario es obligatorio");
        }
        value = value.trim();
        if (value.length() > MAX_LENGTH) {
            throw new InvalidNotificationDataException(
                    "El destinatario no puede superar %d caracteres".formatted(MAX_LENGTH));
        }
    }

    public static Recipient of(final String value) {
        return new Recipient(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
