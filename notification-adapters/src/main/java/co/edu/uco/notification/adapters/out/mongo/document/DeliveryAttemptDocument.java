package co.edu.uco.notification.adapters.out.mongo.document;

import java.time.Instant;

/**
 * Intento de envío embebido en el documento de la notificación.
 *
 * <p>Va embebido y no en una colección aparte porque siempre se lee junto con su notificación y su
 * número está acotado por la política de reintentos.
 *
 * @param number      número de intento
 * @param providerId  proveedor que lo ejecutó
 * @param outcome     resultado del intento
 * @param detail      código o mensaje del proveedor
 * @param attemptedAt momento del intento
 */
public record DeliveryAttemptDocument(
        int number,
        String providerId,
        String outcome,
        String detail,
        Instant attemptedAt) {
}
