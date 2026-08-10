package co.edu.uco.notification.core.entity;

import java.time.Instant;

/**
 * Registro de un intento de envío.
 *
 * <p>Guarda qué proveedor lo ejecutó y cómo terminó. La distinción entre fallo recuperable y
 * definitivo es la que decide si la notificación vuelve a la cola o queda aislada, así que se
 * modela de forma explícita y no se deduce del texto del error.
 *
 * @param number        número de intento, empezando en uno
 * @param providerId    proveedor que ejecutó el envío
 * @param outcome       resultado del intento
 * @param detail        código o mensaje devuelto por el proveedor
 * @param attemptedAt   momento del intento
 */
public record DeliveryAttempt(
        int number,
        String providerId,
        Outcome outcome,
        String detail,
        Instant attemptedAt) {

    /** Resultado de un intento. */
    public enum Outcome {
        /** El proveedor aceptó el envío. */
        SUCCESS,
        /** Fallo transitorio: saturación, tiempo de espera agotado, indisponibilidad momentánea. */
        RECOVERABLE_FAILURE,
        /** Fallo que no mejora al reintentar: datos inválidos, credenciales, destinatario rechazado. */
        PERMANENT_FAILURE
    }

    public static DeliveryAttempt success(
            final int number, final String providerId, final String detail, final Instant attemptedAt) {
        return new DeliveryAttempt(number, providerId, Outcome.SUCCESS, detail, attemptedAt);
    }

    public static DeliveryAttempt recoverableFailure(
            final int number, final String providerId, final String detail, final Instant attemptedAt) {
        return new DeliveryAttempt(number, providerId, Outcome.RECOVERABLE_FAILURE, detail, attemptedAt);
    }

    public static DeliveryAttempt permanentFailure(
            final int number, final String providerId, final String detail, final Instant attemptedAt) {
        return new DeliveryAttempt(number, providerId, Outcome.PERMANENT_FAILURE, detail, attemptedAt);
    }

    public boolean succeeded() {
        return outcome == Outcome.SUCCESS;
    }
}
