package co.edu.uco.notification.core.exception;

import co.edu.uco.notification.core.valueobject.NotificationId;
import co.edu.uco.notification.shared.error.ErrorCode;

/** No existe una notificación con el identificador solicitado. */
public class NotificationNotFoundException extends DomainException {

    public NotificationNotFoundException(final NotificationId id) {
        super(ErrorCode.NOTIFICATION_NOT_FOUND, "No existe la notificación: " + id);
    }
}
