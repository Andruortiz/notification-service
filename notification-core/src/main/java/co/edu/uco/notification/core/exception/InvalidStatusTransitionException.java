package co.edu.uco.notification.core.exception;

import co.edu.uco.notification.core.valueobject.NotificationStatus;
import co.edu.uco.notification.shared.error.ErrorCode;

/**
 * Se intentó una transición que la máquina de estados no contempla.
 *
 * <p>Se lanza en lugar de ignorar la transición en silencio: un estado incoherente detectado
 * tarde es mucho más caro de diagnosticar que un fallo inmediato.
 */
public class InvalidStatusTransitionException extends DomainException {

    public InvalidStatusTransitionException(final NotificationStatus from, final NotificationStatus to) {
        super(ErrorCode.INVALID_STATUS_TRANSITION,
                "Transición de estado no permitida: %s -> %s".formatted(from, to));
    }
}
