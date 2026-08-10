package co.edu.uco.notification.core.valueobject;

import co.edu.uco.notification.core.exception.InvalidNotificationDataException;

/**
 * Referencia que el sistema cliente asigna a su propia solicitud.
 *
 * <p>Es la base de la idempotencia: dos solicitudes del mismo cliente con el mismo valor se
 * consideran la misma y no generan un envío duplicado.
 */
public record ExternalId(String value) {

    private static final int MAX_LENGTH = 120;

    public ExternalId {
        if (value == null || value.isBlank()) {
            throw new InvalidNotificationDataException("La referencia externa es obligatoria");
        }
        value = value.trim();
        if (value.length() > MAX_LENGTH) {
            throw new InvalidNotificationDataException(
                    "La referencia externa no puede superar %d caracteres".formatted(MAX_LENGTH));
        }
    }

    public static ExternalId of(final String value) {
        return new ExternalId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
