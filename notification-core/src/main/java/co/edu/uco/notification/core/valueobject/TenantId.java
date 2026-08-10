package co.edu.uco.notification.core.valueobject;

import co.edu.uco.notification.core.exception.InvalidNotificationDataException;

/**
 * Identificador del sistema cliente que solicita la notificación.
 *
 * <p>Es la clave del aislamiento entre clientes: toda consulta se acota por este valor, de modo
 * que un sistema nunca puede leer las notificaciones de otro.
 */
public record TenantId(String value) {

    public TenantId {
        if (value == null || value.isBlank()) {
            throw new InvalidNotificationDataException("El identificador del cliente es obligatorio");
        }
        value = value.trim();
    }

    public static TenantId of(final String value) {
        return new TenantId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
