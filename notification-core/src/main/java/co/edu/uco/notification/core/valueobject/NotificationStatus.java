package co.edu.uco.notification.core.valueobject;

/**
 * Estados por los que atraviesa una notificación.
 *
 * <p>La máquina de estados es deliberadamente pequeña: cuantos menos estados, menos ambigüedad al
 * diagnosticar un envío. Las transiciones permitidas las gobierna
 * {@code co.edu.uco.notification.core.service.StatusTransitionPolicy}.
 *
 * <pre>
 *   PENDING ──► IN_PROCESS ──► DELIVERED
 *      ▲             │
 *      │             ├──────► RECOVERABLE ──┐
 *      └─────────────┘                      │
 *      └──────────────────────────────────┘
 *                    └──────► FAILED
 *   PENDING ────────────────► DISCARDED
 * </pre>
 */
public enum NotificationStatus {

    /** Aceptada y encolada, a la espera de despacho. */
    PENDING,

    /** El despachador la tomó y está ejecutando el envío. */
    IN_PROCESS,

    /** El proveedor confirmó la aceptación del envío. Estado final. */
    DELIVERED,

    /** El envío falló por una causa transitoria y volverá a intentarse. */
    RECOVERABLE,

    /** El envío falló de forma definitiva. Queda aislada para revisión. Estado final. */
    FAILED,

    /** Se descartó sin intentar el envío: venció su vigencia o el destinatario no acepta el canal. */
    DISCARDED;

    /** Indica si el estado cierra el ciclo de vida y no admite más transiciones. */
    public boolean isFinal() {
        return this == DELIVERED || this == FAILED || this == DISCARDED;
    }

    /** Indica si desde este estado la notificación puede volver a la cola. */
    public boolean allowsRetry() {
        return this == RECOVERABLE;
    }
}
