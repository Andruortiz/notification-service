package co.edu.uco.notification.core.valueobject;

import co.edu.uco.notification.core.exception.InvalidNotificationDataException;

import java.util.UUID;

/** Identificador único de una notificación dentro del componente. */
public record NotificationId(String value) {

    public NotificationId {
        if (value == null || value.isBlank()) {
            throw new InvalidNotificationDataException("El identificador de la notificación es obligatorio");
        }
        value = value.trim();
    }

    public static NotificationId generate() {
        return new NotificationId(UUID.randomUUID().toString());
    }

    public static NotificationId of(final String value) {
        return new NotificationId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
