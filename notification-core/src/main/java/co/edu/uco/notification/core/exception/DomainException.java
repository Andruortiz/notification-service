package co.edu.uco.notification.core.exception;

import co.edu.uco.notification.shared.error.ErrorCode;

/**
 * Raíz de las excepciones de dominio.
 *
 * <p>Toda violación de una regla de negocio se expresa con una excepción de esta jerarquía, que
 * lleva su código de error asociado. La capa de entrada la traduce a una respuesta HTTP sin
 * necesidad de interpretar el mensaje.
 */
public abstract class DomainException extends RuntimeException {

    private final transient ErrorCode errorCode;

    protected DomainException(final ErrorCode errorCode, final String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }
}
