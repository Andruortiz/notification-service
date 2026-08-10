package co.edu.uco.notification.shared.correlation;

import java.util.UUID;

/**
 * Identificador que acompaña a una solicitud durante todo su recorrido, de modo que los registros
 * emitidos por la API, el despachador y el proveedor puedan enlazarse entre sí.
 *
 * @param value valor no vacío del identificador
 */
public record CorrelationId(String value) {

    public static final String HEADER = "X-Correlation-Id";

    public CorrelationId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("El identificador de correlación no puede ser vacío");
        }
        value = value.trim();
    }

    public static CorrelationId generate() {
        return new CorrelationId(UUID.randomUUID().toString());
    }

    /** Devuelve el valor recibido, o uno nuevo si el sistema cliente no lo envió. */
    public static CorrelationId ofNullable(final String candidate) {
        return candidate == null || candidate.isBlank() ? generate() : new CorrelationId(candidate);
    }

    @Override
    public String toString() {
        return value;
    }
}
