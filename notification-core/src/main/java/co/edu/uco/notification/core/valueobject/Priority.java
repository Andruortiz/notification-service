package co.edu.uco.notification.core.valueobject;

import java.util.Locale;

/**
 * Urgencia de la notificación, que determina su orden de despacho en la cola.
 *
 * <p>A diferencia del canal, sí es un conjunto cerrado: son categorías de negocio estables que no
 * cambian al integrar un proveedor nuevo.
 */
public enum Priority {

    HIGH(10),
    MEDIUM(5),
    LOW(1);

    private final int weight;

    Priority(final int weight) {
        this.weight = weight;
    }

    /** Peso que se traslada a la cola para ordenar el despacho. */
    public int weight() {
        return weight;
    }

    /** Convierte el valor recibido, aplicando {@link #MEDIUM} cuando no se indica ninguno. */
    public static Priority fromNullable(final String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return MEDIUM;
        }
        return switch (candidate.trim().toUpperCase(Locale.ROOT)) {
            case "HIGH", "ALTA", "CRITICA" -> HIGH;
            case "LOW", "BAJA" -> LOW;
            default -> MEDIUM;
        };
    }
}
