package co.edu.uco.notification.core.exception;

import co.edu.uco.notification.shared.error.ErrorCode;

/** Los datos recibidos no permiten construir una notificación válida. */
public class InvalidNotificationDataException extends DomainException {

    public InvalidNotificationDataException(final String message) {
        super(ErrorCode.INVALID_NOTIFICATION_DATA, message);
    }
}
