package co.edu.uco.notification.core.service;

import co.edu.uco.notification.core.exception.InvalidNotificationDataException;

import java.time.Duration;

/**
 * Política de reintentos ante fallos recuperables, con espera creciente.
 *
 * <p>Es lógica de negocio pura y no depende de ningún mecanismo de reintento concreto: la aplica
 * igual el consumidor de la cola que el reconciliador programado.
 *
 * @param maxAttempts    número máximo de intentos, incluido el primero
 * @param initialBackoff espera antes del primer reintento
 * @param multiplier     factor por el que crece la espera en cada reintento
 */
public record RetryPolicy(int maxAttempts, Duration initialBackoff, double multiplier) {

    private static final Duration MAX_BACKOFF = Duration.ofMinutes(30);

    public RetryPolicy {
        if (maxAttempts < 1) {
            throw new InvalidNotificationDataException("El número máximo de intentos debe ser al menos 1");
        }
        if (initialBackoff == null || initialBackoff.isNegative()) {
            throw new InvalidNotificationDataException("La espera inicial no puede ser negativa");
        }
        if (multiplier < 1) {
            throw new InvalidNotificationDataException("El factor de crecimiento debe ser al menos 1");
        }
    }

    /** Política por defecto cuando el canal no declara una propia. */
    public static RetryPolicy defaultPolicy() {
        return new RetryPolicy(3, Duration.ofSeconds(30), 2.0);
    }

    /** Indica si queda margen para un intento más después de los ya realizados. */
    public boolean shouldRetry(final int attemptsMade) {
        return attemptsMade < maxAttempts;
    }

    /**
     * Espera antes del intento indicado, acotada a media hora para que un fallo prolongado no
     * programe reintentos a días vista.
     *
     * @param attemptNumber número del intento que se va a ejecutar, empezando en uno
     */
    public Duration backoffFor(final int attemptNumber) {
        if (attemptNumber <= 1) {
            return Duration.ZERO;
        }
        final double factor = Math.pow(multiplier, attemptNumber - 2.0);
        final Duration computed = Duration.ofMillis(Math.round(initialBackoff.toMillis() * factor));
        return computed.compareTo(MAX_BACKOFF) > 0 ? MAX_BACKOFF : computed;
    }
}
